package com.razorpay.experiment.repository;

import com.razorpay.experiment.model.ExperimentSnapshot;

public interface SnapshotRepository {

    ExperimentSnapshot get();

    void publish(ExperimentSnapshot snapshot);
}
