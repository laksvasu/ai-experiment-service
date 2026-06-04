package com.razorpay.experiment.service;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.RolloutConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRolloutServiceTest {

    private final RolloutService rolloutService = new DefaultRolloutService();

    @Test
    void hundredPercentRolloutAlwaysIn() {
        Experiment experiment = experimentWithRollout(100);
        EvaluationDecision decision = rolloutService.evaluateRollout(experiment, "any-user");
        assertTrue(decision.isInExperiment());
        assertEquals(EvaluationReason.ROLLOUT_IN, decision.getReason());
    }

    @Test
    void zeroPercentRolloutAlwaysOut() {
        Experiment experiment = experimentWithRollout(0);
        EvaluationDecision decision = rolloutService.evaluateRollout(experiment, "any-user");
        assertFalse(decision.isInExperiment());
        assertEquals(EvaluationReason.ROLLOUT_OUT, decision.getReason());
    }

    @Test
    void blankCustomerKeyIsOut() {
        Experiment experiment = experimentWithRollout(100);
        EvaluationDecision decision = rolloutService.evaluateRollout(experiment, "  ");
        assertFalse(decision.isInExperiment());
        assertEquals(EvaluationReason.DEFAULT_OUT, decision.getReason());
    }

    @Test
    void stickyForSameCustomerKey() {
        Experiment experiment = experimentWithRollout(50);
        EvaluationDecision first = rolloutService.evaluateRollout(experiment, "sticky-user");
        EvaluationDecision second = rolloutService.evaluateRollout(experiment, "sticky-user");
        assertEquals(first.isInExperiment(), second.isInExperiment());
        assertEquals(first.getBucket(), second.getBucket());
    }

    private Experiment experimentWithRollout(int percentage) {
        return new Experiment(
                "test-exp",
                Environment.PROD,
                "",
                ExperimentStatus.ACTIVE,
                null,
                new RolloutConfig(percentage, "salt", false),
                1L,
                1L,
                1L
        );
    }
}
