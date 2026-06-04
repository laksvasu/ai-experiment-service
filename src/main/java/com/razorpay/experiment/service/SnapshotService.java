package com.razorpay.experiment.service;

import com.razorpay.experiment.model.ExperimentSnapshot;

public interface SnapshotService {

    void init();

    void refresh();

    ExperimentSnapshot getCurrentSnapshot();

    long currentVersion();
}
