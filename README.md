# AI Experiment Service

In-process feature-flag and A/B experiment service for Java 8 applications. Configure experiments via CRUD APIs, evaluate enrollment at runtime from an in-memory snapshot (no database access on the hot path).

## Features

- **Experiment CRUD** — Create, read, update, delete, and list experiments per environment (DEV, STAGING, PROD)
- **Targeting rules** — Attribute-based conditions with priority ordering
- **Percentage rollout** — Deterministic bucketing via MurmurHash3 (stable per customer + salt)
- **Runtime evaluation** — Single API: `customerKey` + `experimentName` → `inExperiment` (boolean) + reason
- **Thread-safe snapshots** — Config changes rebuild an atomic snapshot; evaluate reads snapshot only
- **H2 in-memory persistence** — JDBC-backed config store (cleared on JVM exit in v1)

## Requirements

- Java 8+
- Maven 3.6+

## Quick start

```bash
# Build and run all tests
mvn test

# Manual smoke demo (20% rollout + one evaluation)
mvn -q exec:java -Dexec.mainClass="com.razorpay.experiment.ExperimentServiceDemo"
```

## Usage

Wire the service through `ExperimentApplicationContext`:

```java
ExperimentApplicationContext context = ExperimentApplicationContext.createDefault();
try {
    ExperimentCrudApi crud = context.getExperimentCrudApi();
    ExperimentEvaluateApi evaluate = context.getExperimentEvaluateApi();

    // Create experiment with 20% rollout
    CreateExperimentRequest create = new CreateExperimentRequest();
    create.setName("new-checkout");
    create.setEnvironment(Environment.PROD);
    RolloutConfigDto rollout = new RolloutConfigDto();
    rollout.setPercentage(20);
    rollout.setBucketingSalt("my-salt");
    create.setRollout(rollout);
    crud.createExperiment(create);

    // Evaluate enrollment for a customer
    EvaluateExperimentRequest req = new EvaluateExperimentRequest();
    req.setCustomerKey("customer-123");
    req.setExperimentName("new-checkout");
    req.setEnvironment(Environment.PROD);
    EvaluateExperimentResponse res = evaluate.evaluateExperiment(req);
    // res.isInExperiment(), res.getReason(), res.getSnapshotVersion()
} finally {
    context.shutdown();
}
```

## Architecture

```
Config (CRUD)                              Runtime (evaluate)
─────────────                              ──────────────────
ExperimentCrudApi                          ExperimentEvaluateApi
    → ExperimentCrudEndpoint                   → ExperimentEvaluateEndpoint
        → ExperimentCrudService                    → ExperimentEvaluateService
        → H2ExperimentRepository (JDBC)            → SnapshotRepository (read only)
        → SnapshotService                          → RuleEvaluationService
            → build ExperimentSnapshot                 → RolloutService
            → AtomicSnapshotRepository.publish()
```

- **CRUD path:** Persists to H2, then rebuilds and publishes `ExperimentSnapshot`
- **Evaluate path:** Reads snapshot only — no JDBC per request
- **Rollout:** `MurmurHash3(customerKey + salt) % 100 < percentage`

## Project layout

| Path | Purpose |
|------|---------|
| `src/main/java/com/razorpay/experiment/api/` | Public host-facing contracts |
| `src/main/java/com/razorpay/experiment/controller/` | Request validation and mapping |
| `src/main/java/com/razorpay/experiment/service/` | Business logic (CRUD, evaluate, rollout, rules) |
| `src/main/java/com/razorpay/experiment/repository/` | H2 JDBC + atomic snapshot store |
| `src/main/java/com/razorpay/experiment/dto/` | API request/response types |
| `src/main/resources/schema.sql` | H2 schema |
| `src/test/java/` | Unit and E2E tests |

## Documentation

- [PLAN.md](PLAN.md) — Low-level design, schema, API contracts, evaluation flow
- [TESTING.md](TESTING.md) — Test pyramid, E2E flows, manual verification

## License

Internal / reference implementation — see repository owner for usage terms.
