# Experiment Service — Chat History

Cursor agent conversation digest for this repo. Use with [`PLAN.md`](PLAN.md) (architecture / LLD) and [`TESTING.md`](TESTING.md) (how to run and verify).

> **Note:** Cursor did not persist `agent-transcripts` for this workspace before this file was created. Earlier turns below are reconstructed from deliverables (`PLAN.md`, Java sources, tests). New sessions can append under [Session log](#session-log).

---

## Project summary (from conversation)

| Topic | Outcome |
|-------|---------|
| **Goal** | In-process **experiment / A–B enrollment** service (interview-style) |
| **Stack** | Java 8, H2 in-memory, HikariCP, Gson/Jackson for JSON CLOBs |
| **Pattern** | CRUD → H2 → snapshot refresh; runtime evaluate reads snapshot only (no JDBC per request) |
| **Hashing** | MurmurHash3 → `bucket % 100` for percentage rollout |
| **Docs** | `PLAN.md` (LLD), `TESTING.md` (test guide), this file (chat digest) |
| **Code** | Full package under `com.razorpay.experiment` + unit + E2E tests |

---

## Conversation arc (reconstructed)

### 1 — Scope and architecture

**User (inferred):** Design an experiment service: configure experiments, evaluate whether a `customerKey` is in an experiment, support targeting rules and percentage rollout.

**Assistant:** Proposed layered design:

- **Config path:** `ExperimentCrudApi` → `ExperimentCrudService` → `H2ExperimentRepository` → `SnapshotService.refresh()`
- **Runtime path:** `ExperimentEvaluateApi` → read `ExperimentSnapshot` from `AtomicSnapshotRepository` → rules → rollout
- **Never throw on evaluate;** return `inExperiment=false` with `EvaluationReason` (`NOT_FOUND`, `PAUSED`, `ERROR_DEFAULT`, etc.)

Captured in `PLAN.md` (architecture diagram, APIs, package layout).

---

### 2 — Persistence and snapshot

**User (inferred):** Use H2 in-memory; keep runtime fast.

**Assistant:**

- `experiments` table with composite PK `(name, environment)`
- JSON in CLOBs: `targeting_rules_json`, `rollout_json`
- Optimistic locking via `version` on UPDATE
- `ExperimentSnapshot` built with `SELECT *` after every successful CRUD write
- `AtomicReference` publish for lock-free runtime reads

---

### 3 — APIs and domain model

**Assistant documented:**

| API | Endpoint (logical) | Purpose |
|-----|-------------------|---------|
| CREATE | `POST /experiments` | New experiment |
| READ | `GET /experiments/{name}` | Fetch by name + env |
| UPDATE | `PUT /experiments/{name}` | Update with version check |
| DELETE | `DELETE /experiments/{name}` | Remove |
| LIST | `GET /experiments` | Optional env filter |
| EVALUATE | `POST /experiments/evaluate` | `customerKey` + `experimentName` → `inExperiment` |

**Evaluation pipeline:**

1. Snapshot lookup → `NOT_FOUND` if missing  
2. `PAUSED` → false  
3. Targeting rules (priority order) → `TARGETING_MATCH`  
4. Rollout (MurmurHash3) → `ROLLOUT_IN` / `ROLLOUT_OUT`  
5. Else → `DEFAULT_OUT`

---

### 4 — Implementation

**Assistant:** Implemented under `src/main/java/com/razorpay/experiment/`:

- `api`, `controller`, `service`, `repository`, `db`, `dto`, `model`, `enums`, `mapper`
- `ExperimentApplicationContext` for wiring
- `ExperimentServiceDemo` for manual smoke
- Exceptions: `InvalidExperimentConfigException`, `VersionConflictException`, etc.

---

### 5 — Testing plan

**User (inferred):** Document how to test user flows and unit behavior.

**Assistant:** Added `TESTING.md` with:

- Test pyramid (`MurmurHash3Test`, `DefaultRolloutServiceTest`, `DefaultRuleEvaluationServiceTest`, `ExperimentValidatorTest`, `ExperimentUserFlowIT`)
- Flow-by-flow mapping (create + rollout, targeting, pause, NOT_FOUND, env isolation, invalid config)
- `mvn test` commands and interview demo script

`PLAN.md` § Testing Plan cross-links to `TESTING.md`.

---

### 6 — Chat history export (this session)

**User:** Make my chat history as well in `.MD` file.

**Assistant:** Created this `CHAT_HISTORY.md` and linked it from `PLAN.md`.

---

## Key decisions (chat → design)

| Decision | Rationale |
|----------|-----------|
| Snapshot separate from H2 | Sub-ms evaluate; no SQL on hot path |
| Full snapshot refresh on CRUD | Simple v1; fine for thousands of experiments |
| MurmurHash3 + optional salt / experiment name in hash | Stable buckets; optional cross-experiment bucket sharing |
| Targeting before rollout | Business rules override percentage |
| Reject bad config on CRUD | Runtime fail-safe to `false` |
| H2 `DB_CLOSE_DELAY=-1` | Keep mem DB for JVM lifetime |

---

## Deliverables map

| Artifact | Location |
|----------|----------|
| LLD / architecture | `PLAN.md` |
| Testing guide | `TESTING.md` |
| Chat digest | `CHAT_HISTORY.md` (this file) |
| Schema | `src/main/resources/schema.sql` |
| E2E flows | `src/test/java/.../ExperimentUserFlowIT.java` |
| Demo | `ExperimentServiceDemo.java` |

---

## Session log

### 2026-05-19 — Export chat history

**User**

> Make my chat history as well in .MD file

**Assistant**

- Searched Cursor `agent-transcripts` for this workspace (none saved yet).
- Reconstructed prior conversation arc from `PLAN.md`, `TESTING.md`, and implementation.
- Added `CHAT_HISTORY.md` and cross-link from `PLAN.md`.

---

## How to append future chats

When Cursor saves transcripts under:

`~/.cursor/projects/Users-lakshman-IdeaProjects-razorpay/agent-transcripts/<id>/<id>.jsonl`

each line is JSON with `role` (`user` / `assistant`) and `message.content[].text`. Append a new dated section under [Session log](#session-log) with the user question and a short assistant summary (omit long tool traces).

---

*Companion to `PLAN.md` and `TESTING.md` — conversation context for the Experiment Service build.*
