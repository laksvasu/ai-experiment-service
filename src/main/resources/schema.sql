CREATE TABLE IF NOT EXISTS experiments (
    name                  VARCHAR(255) NOT NULL,
    environment           VARCHAR(32)  NOT NULL,
    description           VARCHAR(1024),
    status                VARCHAR(16)  NOT NULL,
    targeting_rules_json  CLOB,
    rollout_json          CLOB,
    version               BIGINT       NOT NULL,
    created_at            BIGINT       NOT NULL,
    updated_at            BIGINT       NOT NULL,
    PRIMARY KEY (name, environment)
);

CREATE INDEX IF NOT EXISTS idx_experiments_environment ON experiments (environment);
