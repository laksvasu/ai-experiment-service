package com.razorpay.experiment.model;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.ExperimentStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Experiment {

    private final String name;
    private final Environment environment;
    private final String description;
    private final ExperimentStatus status;
    private final List<TargetingRule> targetingRules;
    private final RolloutConfig rollout;
    private final long version;
    private final long createdAt;
    private final long updatedAt;
    private final boolean killSwitch;

    public Experiment(String name,
                      Environment environment,
                      String description,
                      ExperimentStatus status,
                      List<TargetingRule> targetingRules,
                      RolloutConfig rollout,
                      long version,
                      long createdAt,
                      long updatedAt, boolean killSwitch) {
        this.name = name;
        this.environment = environment;
        this.description = description != null ? description : "";
        this.status = status;
        this.targetingRules = targetingRules == null
                ? Collections.<TargetingRule>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(targetingRules));
        this.rollout = rollout;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.killSwitch = killSwitch;
    }

    public String getName() {
        return name;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getDescription() {
        return description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public List<TargetingRule> getTargetingRules() {
        return targetingRules;
    }

    public RolloutConfig getRollout() {
        return rollout;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean hasRollout() {
        return rollout != null;
    }

    public boolean hasTargetingRules() {
        return !targetingRules.isEmpty();
    }

    public boolean isKillSwitchEnabled() {
        return killSwitch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Experiment that = (Experiment) o;
        return version == that.version
                && createdAt == that.createdAt
                && updatedAt == that.updatedAt
                && Objects.equals(name, that.name)
                && environment == that.environment
                && Objects.equals(description, that.description)
                && status == that.status
                && Objects.equals(targetingRules, that.targetingRules)
                && Objects.equals(rollout, that.rollout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, environment, description, status, targetingRules, rollout, version, createdAt, updatedAt);
    }
}
