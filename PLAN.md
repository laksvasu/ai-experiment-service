# Experiment Service — LLD

**Stack:** Java 8 · In-process · Thread-safe · **H2 in-memory DB**  
**Pattern:** Controller → Service → Repository (JDBC) · DTO at boundary · Runtime evaluation via snapshot (no SQL on hot path)

**Dependencies:** `com.h2database:h2` · `com.zaxxer:HikariCP` (connection pool) · Gson/Jackson (JSON columns)

---

## Architecture (short)

```
Config (CRUD)                              Runtime (evaluate)
─────────────                              ──────────────────
api.ExperimentCrudApi                    api.ExperimentEvaluateApi
    → controller.ExperimentCrudEndpoint        → controller.ExperimentEvaluateEndpoint
        → ExperimentCrudService                    → ExperimentEvaluateService
        → H2ExperimentRepository (JDBC)            → SnapshotRepository (read only)
        → SnapshotService                          → RuleEvaluationService
            → SELECT all from H2                       → RolloutService
            → build ExperimentSnapshot
            → AtomicSnapshotRepository.publish()
```

- **CRUD:** Persists to **H2 in-memory** via `H2ExperimentRepository`; then rebuilds `ExperimentSnapshot`.
- **Runtime:** Single evaluate API — `customerKey` + `experimentName` → `inExperiment` (boolean). Reads **snapshot only** (no JDBC per request).
- **Persistence:** H2 mem DB is source of truth for config; data cleared on JVM exit (v1).
- **Hashing:** MurmurHash3 (32-bit) → `bucket % 100` for percentage rollout.

---

## H2 Database

### Connection config

| Property | Value |
|----------|--------|
| JDBC URL | `jdbc:h2:mem:experiments;DB_CLOSE_DELAY=-1;MODE=MySQL` |
| User | `sa` |
| Password | `""` |
| Pool | HikariCP, max 5 connections |
| Init | `schema.sql` on startup via `ExperimentSchemaInitializer` |

`DB_CLOSE_DELAY=-1` keeps the in-memory DB alive for the JVM lifetime.

### Schema — `experiments` table

| Column | SQL type | Purpose |
|--------|----------|---------|
| `name` | `VARCHAR(255) NOT NULL` | PK (part 1) |
| `environment` | `VARCHAR(32) NOT NULL` | PK (part 2) — DEV / STAGING / PROD |
| `description` | `VARCHAR(1024)` | Metadata |
| `status` | `VARCHAR(16) NOT NULL` | ACTIVE / PAUSED |
| `targeting_rules_json` | `CLOB` | Serialized `List<TargetingRule>` |
| `rollout_json` | `CLOB` | Serialized `RolloutConfig` (nullable) |
| `version` | `BIGINT NOT NULL` | Optimistic locking |
| `created_at` | `BIGINT NOT NULL` | Epoch ms |
| `updated_at` | `BIGINT NOT NULL` | Epoch ms |

**Primary key:** `(name, environment)`

**Indexes:** `idx_experiments_environment` on `(environment)` for LIST queries.

### JSON mapping

| Column | Domain field |
|--------|----------------|
| `targeting_rules_json` | `Experiment.targetingRules` |
| `rollout_json` | `Experiment.rollout` |

Serialize/deserialize in `H2ExperimentRepository` via `ExperimentJsonMapper` (Gson/Jackson).

### CRUD → SQL

| API | SQL |
|-----|-----|
| CREATE | `INSERT INTO experiments (...)` |
| READ | `SELECT * FROM experiments WHERE name = ? AND environment = ?` |
| UPDATE | `UPDATE experiments SET ... WHERE name = ? AND environment = ? AND version = ?` |
| DELETE | `DELETE FROM experiments WHERE name = ? AND environment = ?` |
| LIST | `SELECT * FROM experiments WHERE environment = ?` (or all) |
| Snapshot refresh | `SELECT * FROM experiments` → build `ExperimentSnapshot` |

**On every successful CRUD write:** `SnapshotService.refresh()` → full reload from H2 → `AtomicSnapshotRepository.publish()`.

---

## APIs

### CRUD — Experiment configuration

| Method | Endpoint (logical) | Request | Response |
|--------|-------------------|---------|----------|
| **CREATE** | `POST /experiments` | `CreateExperimentRequest` | `ExperimentResponse` |
| **READ** | `GET /experiments/{name}` | `name`, `environment` (query) | `ExperimentResponse` |
| **UPDATE** | `PUT /experiments/{name}` | `UpdateExperimentRequest` | `ExperimentResponse` |
| **DELETE** | `DELETE /experiments/{name}` | `name`, `environment` (query) | `DeleteExperimentResponse` |
| **LIST** | `GET /experiments` | `environment` (query, optional) | `ListExperimentsResponse` |

Flow: **`api.ExperimentController`** → **`ExperimentControllerImpl`** → **`ExperimentService`** → **`H2ExperimentRepository`** → H2 → **`SnapshotService.refresh()`**.

Validation on write; invalid config rejected. Snapshot visible to runtime within < 5s (immediate on successful refresh).

---

### Runtime — Experiment enrollment (single API)

| Method | Endpoint (logical) | Request | Response |
|--------|-------------------|---------|----------|
| **EVALUATE** | `POST /experiments/evaluate` | `EvaluateExperimentRequest` | `EvaluateExperimentResponse` |

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `customerKey` | String | Yes | Stable identity for bucketing |
| `experimentName` | String | Yes | Experiment to check |
| `environment` | Environment | Yes | DEV / STAGING / PROD |
| `attributes` | Map<String, Object> | No | Targeting context (country, tenantId, …) |

| Response field | Type | Description |
|----------------|------|-------------|
| `inExperiment` | boolean | User in experiment or not |
| `experimentName` | String | Echo |
| `reason` | EvaluationReason | Decision trace |
| `snapshotVersion` | long | Snapshot used for eval |

**Contract:** Never throws. Unknown experiment / error → `inExperiment = false` + structured log.

**Runtime pipeline (no H2):**

1. Read `ExperimentSnapshot` from `SnapshotRepository`.
2. Lookup `(environment, experimentName)` → missing → `NOT_FOUND`.
3. `status == PAUSED` → `PAUSED` → `false`.
4. Targeting rules (priority) → match → `TARGETING_MATCH` → `true`.
5. Rollout → MurmurHash3(`customerKey`) → `ROLLOUT_IN` / `ROLLOUT_OUT`.
6. Else → `DEFAULT_OUT` → `false`.

---

## LLD — Package Structure

```
com.razorpay.experiment
│
├── api
│   ├── ExperimentCrudApi                   # public CRUD contract
│   └── ExperimentEvaluateApi               # public evaluate contract
│
├── controller                            # user endpoints only (no business logic)
│   ├── ExperimentCrudEndpoint              # POST/GET/PUT/DELETE /experiments
│   └── ExperimentEvaluateEndpoint          # POST /experiments/evaluate
│
├── config
│   └── ExperimentApplicationContext        # wiring / bootstrap (not an endpoint)
│
├── service
│   ├── ExperimentCrudService
│   ├── DefaultExperimentCrudService
│   ├── ExperimentEvaluateService
│   ├── DefaultExperimentEvaluateService
│   ├── SnapshotService
│   ├── DefaultSnapshotService
│   ├── RuleEvaluationService
│   ├── DefaultRuleEvaluationService
│   ├── RolloutService
│   ├── DefaultRolloutService
│   └── EvaluationErrorService
│
├── repository
│   ├── ExperimentRepository              # interface
│   ├── H2ExperimentRepository            # JDBC CRUD
│   ├── ExperimentRowMapper               # ResultSet → Experiment
│   ├── SnapshotRepository
│   └── AtomicSnapshotRepository
│
├── db
│   ├── H2DataSourceConfig                # DataSource / HikariCP
│   ├── ExperimentSchemaInitializer       # runs schema.sql
│   └── schema.sql
│
├── dto
│   ├── CreateExperimentRequest
│   ├── UpdateExperimentRequest
│   ├── ExperimentResponse
│   ├── DeleteExperimentResponse
│   ├── ListExperimentsResponse
│   ├── EvaluateExperimentRequest
│   ├── EvaluateExperimentResponse
│   ├── TargetingRuleDto
│   ├── ConditionDto
│   └── RolloutConfigDto
│
├── model
│   ├── Experiment
│   ├── ExperimentSnapshot
│   ├── TargetingRule
│   ├── Condition
│   ├── RolloutConfig
│   └── EvaluationDecision
│
├── enums
│   ├── Environment
│   ├── ComparisonOperator
│   ├── EvaluationReason
│   └── ExperimentStatus
│
└── mapper
    ├── ExperimentMapper                  # DTO ↔ model
    ├── ExperimentJsonMapper              # model ↔ JSON for H2 CLOBs
    └── EvaluationMapper
```

---

## Models (domain — `model` package)

### Experiment

| Field | Type | H2 column |
|-------|------|-----------|
| `name` | String | `name` |
| `environment` | Environment | `environment` |
| `description` | String | `description` |
| `status` | ExperimentStatus | `status` |
| `targetingRules` | List\<TargetingRule\> | `targeting_rules_json` |
| `rollout` | RolloutConfig | `rollout_json` |
| `version` | long | `version` |
| `createdAt` | long | `created_at` |
| `updatedAt` | long | `updated_at` |

### TargetingRule

| Field | Type | Purpose |
|-------|------|---------|
| `id` | String | Trace / logging |
| `priority` | int | Lower = evaluated first |
| `conditions` | List\<Condition\> | ANDed |
| `matchInExperiment` | boolean | Match ⇒ in experiment |

### Condition

| Field | Type | Purpose |
|-------|------|---------|
| `attribute` | String | Runtime `attributes` key |
| `operator` | ComparisonOperator | EQ, IN, … |
| `operand` | Object | Expected value |

### RolloutConfig

| Field | Type | Purpose |
|-------|------|---------|
| `percentage` | int | 0–100 |
| `bucketingSalt` | String | MurmurHash3 salt |
| `shareBucketAcrossExperiments` | boolean | Include experiment name in hash |

### ExperimentSnapshot

| Field | Type | Purpose |
|-------|------|---------|
| `experimentsByEnv` | Map\<Environment, Map\<String, Experiment\>\> | Built from H2 `SELECT *`; immutable |
| `snapshotVersion` | long | Incremented on refresh |
| `createdAt` | long | Epoch ms |

**Not stored in H2** — derived in-memory on each refresh for lock-free runtime reads.

### EvaluationDecision (internal)

| Field | Type | Purpose |
|-------|------|---------|
| `inExperiment` | boolean | Result |
| `reason` | EvaluationReason | Source |
| `matchedRuleId` | String | Optional |
| `bucket` | Integer | Optional |

---

## DTOs (`dto` package)

### CreateExperimentRequest

| Field | Type | Required |
|-------|------|----------|
| `name` | String | Yes |
| `environment` | Environment | Yes |
| `description` | String | No |
| `status` | ExperimentStatus | No (default ACTIVE) |
| `targetingRules` | List\<TargetingRuleDto\> | No |
| `rollout` | RolloutConfigDto | No |

### UpdateExperimentRequest

Create fields + `version` (required; matched in SQL `WHERE version = ?`).

### ExperimentResponse / DeleteExperimentResponse / ListExperimentsResponse

Unchanged — mirror persisted `Experiment` from H2.

### EvaluateExperimentRequest / EvaluateExperimentResponse

| Request | Response |
|---------|----------|
| `customerKey`, `experimentName`, `environment`, `attributes?` | `inExperiment`, `experimentName`, `reason`, `snapshotVersion` |

---

## Enums

| Enum | Values |
|------|--------|
| `Environment` | DEV, STAGING, PROD |
| `ExperimentStatus` | ACTIVE, PAUSED |
| `ComparisonOperator` | EQ, NEQ, IN, NOT_IN, GT, GTE, LT, LTE |
| `EvaluationReason` | TARGETING_MATCH, ROLLOUT_IN, ROLLOUT_OUT, DEFAULT_OUT, NOT_FOUND, PAUSED, ERROR_DEFAULT |

---

## Layer responsibilities

| Layer | CRUD | Runtime |
|-------|------|---------|
| **API** | `api.ExperimentCrudApi` | `api.ExperimentEvaluateApi` |
| **Controller (endpoints only)** | `ExperimentCrudEndpoint` | `ExperimentEvaluateEndpoint` |
| **Service (business logic)** | `ExperimentCrudService` | `ExperimentEvaluateService` |
| **Repository (DB only)** | `H2ExperimentRepository` | `AtomicSnapshotRepository` (via `SnapshotService`) |

### H2ExperimentRepository

| Method | Behavior |
|--------|----------|
| `save(Experiment)` | INSERT |
| `update(Experiment)` | UPDATE with version check |
| `findByNameAndEnv(name, env)` | SELECT one |
| `findAll()` / `findByEnvironment(env)` | SELECT many |
| `delete(name, env)` | DELETE |

Uses `DataSource` from `H2DataSourceConfig`; maps rows via `ExperimentRowMapper` + `ExperimentJsonMapper`.

### SnapshotService

| Method | Behavior |
|--------|----------|
| `refresh()` | `SELECT *` from H2 → build `ExperimentSnapshot` → `AtomicSnapshotRepository.publish()` |
| `init()` | Called on app startup — load initial snapshot |

### ExperimentEvaluationService

Reads **only** `SnapshotRepository` — never calls H2 at runtime.

---

## Repository & storage summary

| Component | Technology | When used |
|-----------|------------|-----------|
| `H2ExperimentRepository` | H2 in-memory (JDBC) | CRUD + snapshot refresh |
| `AtomicSnapshotRepository` | `AtomicReference<ExperimentSnapshot>` | Every runtime evaluate |
| `schema.sql` | DDL | Startup |

**Write path:** Controller → Service → H2 → SnapshotService.refresh() → AtomicReference.set().

**Read path (runtime):** Controller → EvaluationService → SnapshotRepository.get() (no SQL).

---

## Concurrency

| Concern | Strategy |
|---------|----------|
| H2 writes | Short JDBC transactions per CRUD; HikariCP pool |
| H2 read (refresh) | Single `SELECT *` under snapshot rebuild; off CRUD thread optional |
| Runtime evaluate | Lock-free read of immutable `ExperimentSnapshot` |
| Version conflict | UPDATE `WHERE version = ?`; 0 rows → reject update |
| Publish | `AtomicReference` swap after snapshot build |

---

## Key decisions

| Topic | Decision |
|-------|----------|
| Persistence | **H2 in-memory** — source of truth for experiments |
| Runtime reads | **ExperimentSnapshot** — no per-request JDBC |
| Config vs runtime | CRUD → H2; evaluate → snapshot only |
| Customer identity | Host passes `customerKey` |
| Targeting vs rollout | Targeting first; rollout if no match |
| Misconfiguration | Reject on CRUD; runtime → `false` |
| Hash | MurmurHash3, `bucket % 100` |
| JSON in DB | CLOB columns for rules/rollout (simple v1) |

---

## H2 vs snapshot — tradeoffs

| | H2 (CRUD store) | ExperimentSnapshot (runtime) |
|---|-----------------|------------------------------|
| **Role** | Durable config within JVM lifetime | Fast eval read model |
| **Pros** | SQL CRUD, familiar, easy LIST/filter | Sub-ms evaluate, thread-safe |
| **Cons** | JDBC on write/refresh | Full reload on each CRUD change |
| **Scale** | Fine for thousands of experiments | 50k+ evals/sec per JVM |

---

## Dependency flow

```
api.ExperimentController → controller.ExperimentControllerImpl
    → ExperimentService → H2ExperimentRepository → H2
                        → SnapshotService → H2 (SELECT *) → SnapshotRepository

api.ExperimentEvaluationController → controller.ExperimentEvaluationControllerImpl
    → ExperimentEvaluationService → SnapshotRepository
                                 → RuleEvaluationService
                                 → RolloutService
```

---

## Startup sequence

1. `H2DataSourceConfig` — create DataSource + pool.
2. `ExperimentSchemaInitializer` — run `schema.sql`.
3. `SnapshotService.init()` — `SELECT *` → first `ExperimentSnapshot` publish.
4. `ExperimentApplicationContext` — wire API impls (interfaces exposed from `api` package).

---

## Example flows

**CREATE (20% rollout):**

- `POST /experiments` → INSERT into H2 → `SnapshotService.refresh()` → runtime sees new experiment.

**EVALUATE:**

- `POST /experiments/evaluate` with `customerKey=user-123`, `experimentName=new-checkout` → read snapshot → rollout bucket → `inExperiment=true`.

**UPDATE with version:**

- `PUT` with `version=2` → `UPDATE ... WHERE version=2` → if 0 rows updated, return conflict → no snapshot refresh.

---

## Testing Plan

> **Hands-on guide:** see [`TESTING.md`](TESTING.md) for commands, flow-by-flow steps, manual demo script, and troubleshooting.  
> **Conversation context:** see [`CHAT_HISTORY.md`](CHAT_HISTORY.md) for the Cursor chat digest that led to this design.

### Test pyramid (implemented)

| Layer | Test class | Scope |
|-------|------------|--------|
| **Unit** | `MurmurHash3Test` | Hash stability, bucket 0–99 |
| **Unit** | `DefaultRolloutServiceTest` | % rollout, stickiness, 0%/100% |
| **Unit** | `DefaultRuleEvaluationServiceTest` | Targeting, priority |
| **Unit** | `ExperimentValidatorTest` | Config rejection |
| **E2E** | `ExperimentUserFlowIT` | CRUD → H2 → snapshot → evaluate |

**Run all:** `mvn test` · **Details:** `TESTING.md`

---

### Test setup (common)

1. Start H2: `jdbc:h2:mem:test_experiments;DB_CLOSE_DELAY=-1`.
2. Run `schema.sql` via `ExperimentSchemaInitializer`.
3. `SnapshotService.init()` — empty or seeded DB.
4. Wire context → `api.ExperimentController` + `api.ExperimentEvaluationController`.
5. **Teardown:** drop tables or new in-memory DB per test class.

**Seed helpers:** factory methods for `CreateExperimentRequest`, `EvaluateExperimentRequest`, fixed `customerKey` values with known buckets (precompute MurmurHash3 for deterministic rollout tests).

---

### User flow testing (E2E scenarios)

Each flow = **Arrange (CRUD)** → **Act (evaluate)** → **Assert (response + reason + H2 state)**.

#### Flow 1 — Create experiment and evaluate (rollout only)

| Step | Actor | Action | Expected |
|------|-------|--------|----------|
| 1 | Admin | `POST /experiments` — name `new-checkout`, env `PROD`, rollout 50% | `201`, `ExperimentResponse` with version 1 |
| 2 | System | H2 row exists; snapshot refreshed | `GET` snapshot version incremented |
| 3 | App | `POST /experiments/evaluate` — `customerKey=alice`, `experimentName=new-checkout` | `inExperiment` consistent on repeat calls (sticky) |
| 4 | App | Same call for `customerKey=bob` | May differ from alice (different bucket) |
| 5 | App | Repeat step 3 ten times | Same `inExperiment` every time; same `reason` (ROLLOUT_IN or ROLLOUT_OUT) |

**Pass criteria:** Stickiness for same `customerKey`; distribution roughly 50% over many keys (smoke, not statistical gate).

---

#### Flow 2 — Targeting rule (country) before rollout

| Step | Action | Expected |
|------|--------|----------|
| 1 | `POST /experiments` — rule: `country EQ IN`, rollout 100% | Created |
| 2 | Evaluate `customerKey=u1`, `attributes={country: IN}` | `inExperiment=true`, `reason=TARGETING_MATCH` |
| 3 | Evaluate `customerKey=u2`, `attributes={country: US}` | Rollout decides (100% → `ROLLOUT_IN` if rollout applies) or `DEFAULT_OUT` if no rollout branch — per config |
| 4 | Evaluate with no `country` attribute | No targeting match → rollout or `DEFAULT_OUT` |

**Pass criteria:** Targeting evaluated before rollout; India user in via rule without relying on hash.

---

#### Flow 3 — Read / update / delete lifecycle

| Step | Action | Expected |
|------|--------|----------|
| 1 | `POST` create experiment `feat-a` | Success |
| 2 | `GET /experiments/feat-a?environment=PROD` | Matches created config |
| 3 | `PUT` update rollout 10% → 80%, `version=1` | Success, `version=2` |
| 4 | Evaluate same `customerKey` before vs after | Bucket unchanged (same % input); in/out may flip only if percentage threshold crossed |
| 5 | `PUT` with stale `version=1` | Conflict / rejected; H2 still version 2 |
| 6 | `DELETE /experiments/feat-a` | `deleted=true` |
| 7 | Evaluate same request | `inExperiment=false`, `reason=NOT_FOUND` |

**Pass criteria:** CRUD reflected in H2; snapshot picks up changes; evaluate sees deleted experiment.

---

#### Flow 4 — Paused experiment

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create experiment `status=ACTIVE`, rollout 100% | Created |
| 2 | Evaluate any `customerKey` | `inExperiment=true` (rollout 100%) |
| 3 | `PUT` `status=PAUSED` | Updated in H2 |
| 4 | Evaluate again | `inExperiment=false`, `reason=PAUSED` |

---

#### Flow 5 — Unknown experiment at runtime

| Step | Action | Expected |
|------|--------|----------|
| 1 | Evaluate `experimentName=does-not-exist` (no prior CREATE) | `inExperiment=false`, `reason=NOT_FOUND` |
| 2 | No exception thrown to caller | Response always returned |

---

#### Flow 6 — Environment isolation

| Step | Action | Expected |
|------|--------|----------|
| 1 | `POST` same name `btn-test` in `DEV` (rollout 0%) and `PROD` (rollout 100%) | Both created |
| 2 | Evaluate `environment=DEV` | Out of experiment |
| 3 | Evaluate `environment=PROD` | In experiment |
| 4 | `LIST ?environment=DEV` | Only DEV row |

---

#### Flow 7 — Live config propagation (< 5s)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create experiment; evaluate → record `snapshotVersion` |
| 2 | `PUT` change rollout 0% → 100% | New version in response |
| 3 | Evaluate immediately | `snapshotVersion` ≥ step 2; `inExperiment=true` |
| 4 | Measure time CREATE → evaluate sees new config | < 5s (typically immediate after refresh) |

---

#### Flow 8 — Invalid config rejected (no runtime impact)

| Step | Action | Expected |
|------|--------|----------|
| 1 | `POST` rollout `percentage=150` | Rejected; no H2 row |
| 2 | Evaluate experiment name from step 1 | `NOT_FOUND` |
| 3 | blank `customerKey` evaluate on valid experiment | `inExperiment=false`, `ERROR_DEFAULT` |

---

#### Flow 9 — Concurrent CRUD + evaluate

| Step | Action | Expected |
|------|--------|----------|
| 1 | Thread A: loop evaluate (10k times) | No exceptions |
| 2 | Thread B: `PUT` update rollout mid-flight | Completes |
| 3 | Thread A | Only sees old or new snapshot per call; never torn/partial config |

---

### E2E tests → user flows (implemented in `ExperimentUserFlowIT`)

| Flow | Test method |
|------|-------------|
| 1 — Create + evaluate + delete | `createEvaluateAndDeleteRolloutExperiment` |
| 2 — Targeting before rollout | `targetingRuleMatchesBeforeRollout` |
| 4 — Paused | `pausedExperimentReturnsFalse` |
| 5 — NOT_FOUND | `unknownExperimentReturnsNotFoundWithoutThrowing` |
| 6 — Env isolation | `environmentIsolation` |
| 8 — Invalid config | `invalidCreateIsRejected`, `evaluateWithBlankCustomerKeyFailsSafe` |
| 3 — GET | `getExistingExperiment` |

### User flow test execution order (recommended)

```
mvn test -Dtest=ExperimentUserFlowIT#unknownExperimentReturnsNotFoundWithoutThrowing
mvn test -Dtest=ExperimentUserFlowIT#createEvaluateAndDeleteRolloutExperiment
mvn test -Dtest=ExperimentUserFlowIT#targetingRuleMatchesBeforeRollout
mvn test -Dtest=ExperimentUserFlowIT#environmentIsolation
mvn test -Dtest=ExperimentUserFlowIT#getExistingExperiment
mvn test -Dtest=ExperimentUserFlowIT#pausedExperimentReturnsFalse
mvn test -Dtest=ExperimentUserFlowIT#invalidCreateIsRejected
mvn test -Dtest=ExperimentUserFlowIT#evaluateWithBlankCustomerKeyFailsSafe
mvn test   # full suite
```

---

### Assertions checklist (per evaluate call)

| Assert | Field |
|--------|--------|
| Response never null | `EvaluateExperimentResponse` |
| Boolean present | `inExperiment` |
| Traceable reason | `reason` enum |
| Config generation | `snapshotVersion > 0` |
| Experiment echo | `experimentName` matches request |

---

### Manual / demo script (interview)

1. **Create** `new-checkout` with 20% rollout in PROD.
2. **Evaluate** `customerKey=demo-user` three times → show same result.
3. **List** experiments in PROD → show H2-backed row.
4. **Pause** experiment → evaluate → show `PAUSED`.
5. **Show H2** (optional): query `SELECT name, status, rollout_json FROM experiments`.

---

### Non-functional tests (optional)

| Test | Goal |
|------|------|
| Evaluate latency | p99 < 1ms (snapshot read only) |
| Throughput smoke | 10k evaluates/sec single JVM (no CRUD concurrent) |
| H2 pool exhaustion | CRUD under load does not block evaluate threads |

---

*LLD — H2 in-memory DB for CRUD; ExperimentSnapshot for runtime enrollment checks; Testing Plan for user flows and test layers; see `CHAT_HISTORY.md` for chat digest.*

