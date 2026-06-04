# Experiment Service — Testing Guide

How to run automated tests, map them to user flows, and manually verify CRUD + runtime evaluate.

---

## Quick start

```bash
cd /Users/lakshman/IdeaProjects/razorpay

# All tests
mvn test

# Single test class
mvn test -Dtest=ExperimentUserFlowIT

# Single test method
mvn test -Dtest=ExperimentUserFlowIT#createEvaluateAndDeleteRolloutExperiment

# Manual smoke demo (creates 20% rollout + evaluates one user)
mvn -q exec:java -Dexec.mainClass="com.razorpay.experiment.ExperimentServiceDemo"
```

---

## Test pyramid (what exists today)

```
                    ┌─────────────────────────┐
                    │  ExperimentUserFlowIT   │  ← E2E (CRUD + H2 + snapshot + evaluate)
                    └───────────┬─────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐   ┌──────────▼─────────┐   ┌────────▼────────┐
│ RolloutService │   │ RuleEvaluation     │   │ Experiment      │
│ Test           │   │ ServiceTest        │   │ ValidatorTest   │
└───────┬────────┘   └──────────┬─────────┘   └────────┬────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │   MurmurHash3Test     │  ← hashing primitive
                    └───────────────────────┘
```

| Layer | Test class | What it proves |
|-------|------------|----------------|
| Unit | `MurmurHash3Test` | Bucket 0–99 is stable for same input |
| Unit | `DefaultRolloutServiceTest` | 0% out, 100% in, stickiness, blank key out |
| Unit | `DefaultRuleEvaluationServiceTest` | Country rule, priority, no match |
| Unit | `ExperimentValidatorTest` | Bad % and empty config rejected |
| E2E | `ExperimentUserFlowIT` | Full flows through controllers + H2 |

---

## E2E setup (how every flow test runs)

Each test in `ExperimentUserFlowIT` follows this lifecycle:

```
@BeforeEach
  1. New isolated H2 DB  →  jdbc:h2:mem:test_<uuid>
  2. schema.sql applied
  3. SnapshotService.init()  →  empty snapshot
  4. Endpoints wired via `config.ExperimentApplicationContext` (`api` → `controller` → `service` → `repository`)

@Test
  Arrange  →  experimentController.create(...)
  Act      →  evaluationController.evaluate(...)
  Assert   →  inExperiment, reason, snapshotVersion

@AfterEach
  context.shutdown()
```

**Why a new DB per test?** Tests do not share state; parallel-safe and deterministic.

---

## User flows → automated tests (mapping)

### Flow 1 — Create + rollout + stickiness + delete

| Step | Action | Automated coverage |
|------|--------|-------------------|
| Create 50%/100% rollout | `experimentController.create(...)` | `createEvaluateAndDeleteRolloutExperiment` |
| Evaluate same user twice | Same `inExperiment` | same test (repeat evaluate) |
| Delete | `experimentController.delete(...)` | same test |
| Evaluate after delete | `NOT_FOUND` | same test |

**Run:** `mvn test -Dtest=ExperimentUserFlowIT#createEvaluateAndDeleteRolloutExperiment`

**Expected path:**
```
CREATE (rollout 100%) → H2 INSERT → snapshot refresh
EVALUATE user-123     → ROLLOUT_IN, inExperiment=true
EVALUATE user-123     → same result (sticky)
DELETE                → H2 DELETE → snapshot refresh
EVALUATE user-123     → NOT_FOUND, inExperiment=false
```

---

### Flow 2 — Targeting before rollout

| Step | Expected | Test method |
|------|----------|-------------|
| Rule `country=IN` + rollout 0% | IN user → `TARGETING_MATCH` | `targetingRuleMatchesBeforeRollout` |
| US user with 0% rollout | out (no rule match) | (extend test if needed) |

**Run:** `mvn test -Dtest=ExperimentUserFlowIT#targetingRuleMatchesBeforeRollout`

**Decision order under test:**
```
1. Targeting match?  →  inExperiment=true, TARGETING_MATCH
2. Else rollout      →  bucket < percentage
3. Else              →  DEFAULT_OUT
```

---

### Flow 4 — Paused experiment

| Step | Expected | Test method |
|------|----------|-------------|
| Create ACTIVE, 100% rollout | in | `pausedExperimentReturnsFalse` |
| UPDATE status=PAUSED | H2 updated | same |
| Evaluate | `PAUSED`, inExperiment=false | same |

---

### Flow 5 — Unknown experiment

| Step | Expected | Test method |
|------|----------|-------------|
| Evaluate without CREATE | `NOT_FOUND`, no exception | `unknownExperimentReturnsNotFoundWithoutThrowing` |

---

### Flow 6 — Environment isolation

| Step | Expected | Test method |
|------|----------|-------------|
| Same name in DEV (0%) and PROD (100%) | DEV out, PROD in | `environmentIsolation` |

---

### Flow 8 — Invalid config

| Step | Expected | Test method |
|------|----------|-------------|
| CREATE percentage=200 | `InvalidExperimentConfigException` | `invalidCreateIsRejected` |
| Evaluate blank customerKey | `ERROR_DEFAULT` (fail-safe) | `evaluateWithBlankCustomerKeyFailsSafe` |

---

### Flow 3 — GET lifecycle (partial)

| Step | Test method |
|------|-------------|
| CREATE + GET | `getExistingExperiment` |
| GET wrong env → not found | same |

*Update/delete version conflict tests can be added under `ExperimentUserFlowIT` following the same Arrange–Act–Assert pattern.*

---

## Unit tests (isolated, no H2)

### Rollout / percentage (10% exposure)

`DefaultRolloutServiceTest` validates the core rule:

```
inExperiment = (MurmurHash3(salt:experiment:customerKey) % 100) < percentage
```

| Test | Meaning |
|------|---------|
| `hundredPercentRolloutAlwaysIn` | 100% → everyone in |
| `zeroPercentRolloutAlwaysOut` | 0% → everyone out |
| `stickyForSameCustomerKey` | Same key → same bucket → same in/out |
| `blankCustomerKeyIsOut` | Missing key → not in rollout |

For **10% exposure:** only buckets `0–9` are in; `10–99` get `ROLLOUT_OUT` and `inExperiment=false`.

**Run:** `mvn test -Dtest=DefaultRolloutServiceTest`

---

### Targeting rules

`DefaultRuleEvaluationServiceTest` — no snapshot, no H2.

**Run:** `mvn test -Dtest=DefaultRuleEvaluationServiceTest`

---

### Validation (CRUD guardrails)

`ExperimentValidatorTest` — rejects before H2 write.

**Run:** `mvn test -Dtest=ExperimentValidatorTest`

---

## Manual testing flow (step-by-step)

Use this when demoing or debugging without JUnit.

### 1. Start context

Wire via `ExperimentApplicationContext.createDefault()` (see `ExperimentServiceDemo`).

### 2. Create experiment (admin / CRUD path)

| Field | Example |
|-------|---------|
| name | `new-checkout` |
| environment | `PROD` |
| rollout.percentage | `20` |
| rollout.bucketingSalt | `my-salt` |

Call: `experimentController.create(request)`

**Verify:** response `version=1`; no exception.

### 3. Evaluate (runtime path)

| Field | Example |
|-------|---------|
| customerKey | `customer-123` |
| experimentName | `new-checkout` |
| environment | `PROD` |

Call: `evaluationController.evaluate(request)` **multiple times**

**Verify:**

| Check | Expected |
|-------|----------|
| Same customer, repeated calls | Same `inExperiment` (sticky) |
| `reason` | `ROLLOUT_IN` or `ROLLOUT_OUT` |
| `snapshotVersion` | `> 0` |

### 4. Pause experiment

`update` with `status=PAUSED`, `version=1`, include rollout in request.

Evaluate again → `inExperiment=false`, `reason=PAUSED`.

### 5. Delete experiment

`delete("new-checkout", PROD)` → `deleted=true`

Evaluate → `NOT_FOUND`.

---

## Assertions checklist (every evaluate)

| # | Assert |
|---|--------|
| 1 | Response not null |
| 2 | `experimentName` echoes request |
| 3 | `reason` is one of: `TARGETING_MATCH`, `ROLLOUT_IN`, `ROLLOUT_OUT`, `DEFAULT_OUT`, `NOT_FOUND`, `PAUSED`, `ERROR_DEFAULT` |
| 4 | `snapshotVersion > 0` after at least one CREATE |
| 5 | Repeated evaluate with same `customerKey` → same `inExperiment` (unless config changed) |

---

## CRUD vs runtime — what to test where

| Concern | Test via | Why |
|---------|----------|-----|
| SQL / H2 persistence | `ExperimentUserFlowIT` or future `H2ExperimentRepositoryTest` | JDBC path |
| % rollout math | `DefaultRolloutServiceTest` | Fast, deterministic |
| Rule priority | `DefaultRuleEvaluationServiceTest` | No DB noise |
| Snapshot refresh after CREATE | `ExperimentUserFlowIT` | Proves evaluate sees new config |
| Never throw on evaluate | `unknownExperiment...`, `evaluateWithBlankCustomerKey...` | Production contract |

---

## Recommended run order (CI / local)

```bash
# 1. Fast unit tests
mvn test -Dtest=MurmurHash3Test,DefaultRolloutServiceTest,DefaultRuleEvaluationServiceTest,ExperimentValidatorTest

# 2. Full E2E
mvn test -Dtest=ExperimentUserFlowIT

# 3. Everything
mvn test
```

---

## Adding a new flow test

1. Open `ExperimentUserFlowIT.java`.
2. Add `@Test void myNewFlow()`.
3. Follow **Arrange → Act → Assert**:

```java
// Arrange
experimentController.create(...);

// Act
EvaluateExperimentResponse response = evaluationController.evaluate(...);

// Assert
assertTrue(response.isInExperiment());
assertEquals(EvaluationReason.ROLLOUT_IN, response.getReason());
```

4. Run: `mvn test -Dtest=ExperimentUserFlowIT#myNewFlow`

Use `ExperimentTestFixtures` (test package) for reusable request builders.

---

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| `NOT_FOUND` right after CREATE | Snapshot not refreshed — check `SnapshotService.refresh()` after insert |
| Always `ROLLOUT_OUT` with 100% | Wrong experiment name or environment |
| `InvalidExperimentConfigException` | No rules and no rollout, or percentage ∉ [0,100] |
| `VersionConflictException` | Stale `version` on UPDATE |
| `ERROR_DEFAULT` on evaluate | Blank `customerKey` or internal error — check logs for `experiment_eval_error` |

---

## Interview demo script (2 minutes)

1. `mvn test` — show all green.
2. `mvn -q exec:java -Dexec.mainClass="com.razorpay.experiment.ExperimentServiceDemo"`.
3. Open `ExperimentUserFlowIT#createEvaluateAndDeleteRolloutExperiment` — walk through Arrange / Act / Assert.
4. Open `DefaultRolloutServiceTest` — explain `bucket < percentage` for 10% exposure.

---

*See `PLAN.md` for architecture and `CHAT_HISTORY.md` for how the project was discussed in chat; this file is the hands-on testing companion.*
