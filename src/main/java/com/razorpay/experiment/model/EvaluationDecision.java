package com.razorpay.experiment.model;

import com.razorpay.experiment.enums.EvaluationReason;

public final class EvaluationDecision {

    private final boolean inExperiment;
    private final EvaluationReason reason;
    private final String matchedRuleId;
    private final Integer bucket;

    public EvaluationDecision(boolean inExperiment, EvaluationReason reason) {
        this(inExperiment, reason, null, null);
    }

    public EvaluationDecision(boolean inExperiment, EvaluationReason reason, String matchedRuleId, Integer bucket) {
        this.inExperiment = inExperiment;
        this.reason = reason;
        this.matchedRuleId = matchedRuleId;
        this.bucket = bucket;
    }

    public boolean isInExperiment() {
        return inExperiment;
    }

    public EvaluationReason getReason() {
        return reason;
    }

    public String getMatchedRuleId() {
        return matchedRuleId;
    }

    public Integer getBucket() {
        return bucket;
    }
}
