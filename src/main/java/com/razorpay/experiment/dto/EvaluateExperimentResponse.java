package com.razorpay.experiment.dto;

import com.razorpay.experiment.enums.EvaluationReason;

public class EvaluateExperimentResponse {

    private boolean inExperiment;
    private String experimentName;
    private EvaluationReason reason;
    private long snapshotVersion;

    public EvaluateExperimentResponse() {
    }

    public EvaluateExperimentResponse(boolean inExperiment,
                                      String experimentName,
                                      EvaluationReason reason,
                                      long snapshotVersion) {
        this.inExperiment = inExperiment;
        this.experimentName = experimentName;
        this.reason = reason;
        this.snapshotVersion = snapshotVersion;
    }

    public boolean isInExperiment() {
        return inExperiment;
    }

    public void setInExperiment(boolean inExperiment) {
        this.inExperiment = inExperiment;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public EvaluationReason getReason() {
        return reason;
    }

    public void setReason(EvaluationReason reason) {
        this.reason = reason;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }
}
