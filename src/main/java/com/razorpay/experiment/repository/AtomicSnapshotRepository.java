package com.razorpay.experiment.repository;

import com.razorpay.experiment.model.ExperimentSnapshot;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicSnapshotRepository implements SnapshotRepository {

    private final AtomicReference<ExperimentSnapshot> snapshotRef =
            new AtomicReference<ExperimentSnapshot>(ExperimentSnapshot.empty(0));

    @Override
    public ExperimentSnapshot get() {
        return snapshotRef.get();
    }

    @Override
    public void publish(ExperimentSnapshot snapshot) {
        snapshotRef.set(snapshot);
    }
}
