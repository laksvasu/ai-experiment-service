package com.razorpay.experiment.service;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.exception.ExperimentPersistenceException;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.ExperimentSnapshot;
import com.razorpay.experiment.repository.ExperimentRepository;
import com.razorpay.experiment.repository.SnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultSnapshotService implements SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSnapshotService.class);

    private final ExperimentRepository experimentRepository;
    private final SnapshotRepository snapshotRepository;
    private final AtomicLong versionGenerator = new AtomicLong(0);

    public DefaultSnapshotService(ExperimentRepository experimentRepository,
                                  SnapshotRepository snapshotRepository) {
        this.experimentRepository = experimentRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public void init() {
        refresh();
    }

    @Override
    public void refresh() {
        try {
            List<Experiment> experiments = experimentRepository.findAll();
            Map<Environment, Map<String, Experiment>> byEnv = new EnumMap<Environment, Map<String, Experiment>>(Environment.class);
            for (Environment env : Environment.values()) {
                byEnv.put(env, new HashMap<String, Experiment>());
            }
            for (Experiment experiment : experiments) {
                byEnv.get(experiment.getEnvironment()).put(experiment.getName(), experiment);
            }
            Map<Environment, Map<String, Experiment>> immutableByEnv =
                    new EnumMap<Environment, Map<String, Experiment>>(Environment.class);
            for (Map.Entry<Environment, Map<String, Experiment>> entry : byEnv.entrySet()) {
                immutableByEnv.put(entry.getKey(),
                        java.util.Collections.unmodifiableMap(new HashMap<String, Experiment>(entry.getValue())));
            }
            long version = versionGenerator.incrementAndGet();
            ExperimentSnapshot snapshot = new ExperimentSnapshot(immutableByEnv, version, System.currentTimeMillis());
            snapshotRepository.publish(snapshot);
            log.info("Published experiment snapshot version={} experimentCount={}", version, experiments.size());
        } catch (RuntimeException e) {
            log.error("Failed to refresh experiment snapshot", e);
            throw new ExperimentPersistenceException("Snapshot refresh failed", e);
        }
    }

    @Override
    public ExperimentSnapshot getCurrentSnapshot() {
        return snapshotRepository.get();
    }

    @Override
    public long currentVersion() {
        return snapshotRepository.get().getSnapshotVersion();
    }
}
