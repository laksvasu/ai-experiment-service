package com.razorpay.experiment.model;

import com.razorpay.experiment.enums.Environment;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ExperimentSnapshot {

    private final Map<Environment, Map<String, Experiment>> experimentsByEnv;
    private final long snapshotVersion;
    private final long createdAt;

    public ExperimentSnapshot(Map<Environment, Map<String, Experiment>> experimentsByEnv,
                              long snapshotVersion,
                              long createdAt) {
        Map<Environment, Map<String, Experiment>> copy = new EnumMap<>(Environment.class);
        if (experimentsByEnv != null) {
            for (Map.Entry<Environment, Map<String, Experiment>> entry : experimentsByEnv.entrySet()) {
                copy.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
            }
        }
        this.experimentsByEnv = Collections.unmodifiableMap(copy);
        this.snapshotVersion = snapshotVersion;
        this.createdAt = createdAt;
    }

    public static ExperimentSnapshot empty(long version) {
        return new ExperimentSnapshot(Collections.<Environment, Map<String, Experiment>>emptyMap(), version, System.currentTimeMillis());
    }

    public Optional<Experiment> find(Environment environment, String experimentName) {
        Map<String, Experiment> byName = experimentsByEnv.get(environment);
        if (byName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(experimentName));
    }

    public Map<Environment, Map<String, Experiment>> getExperimentsByEnv() {
        return experimentsByEnv;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
