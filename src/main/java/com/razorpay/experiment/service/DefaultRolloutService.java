package com.razorpay.experiment.service;

import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.RolloutConfig;
import com.razorpay.experiment.util.MurmurHash3;

public class DefaultRolloutService implements RolloutService {

    @Override
    public EvaluationDecision evaluateRollout(Experiment experiment, String customerKey) {
        RolloutConfig rollout = experiment.getRollout();
        if (rollout == null) {
            return new EvaluationDecision(false, EvaluationReason.DEFAULT_OUT);
        }
        if (customerKey == null || customerKey.trim().isEmpty()) {
            return new EvaluationDecision(false, EvaluationReason.DEFAULT_OUT);
        }

        String hashInput = buildHashInput(experiment.getName(), customerKey.trim(), rollout);
        int bucket = MurmurHash3.bucket(hashInput);
        boolean inExperiment = bucket < rollout.getPercentage();
        EvaluationReason reason = inExperiment ? EvaluationReason.ROLLOUT_IN : EvaluationReason.ROLLOUT_OUT;
        return new EvaluationDecision(inExperiment, reason, null, bucket);
    }

    private String buildHashInput(String experimentName, String customerKey, RolloutConfig rollout) {
        String salt = rollout.getBucketingSalt() != null ? rollout.getBucketingSalt() : "";
        if (rollout.isShareBucketAcrossExperiments()) {
            return salt + ":" + customerKey;
        }
        return salt + ":" + experimentName + ":" + customerKey;
    }
}
