package com.razorpay.experiment.service;

import com.razorpay.experiment.enums.ComparisonOperator;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.model.Condition;
import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.TargetingRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRuleEvaluationServiceTest {

    private final RuleEvaluationService ruleEvaluationService = new DefaultRuleEvaluationService();

    @Test
    void matchesCountryRule() {
        TargetingRule rule = new TargetingRule(
                "r1",
                1,
                Collections.singletonList(new Condition("country", ComparisonOperator.EQ, "IN")),
                true
        );
        Experiment experiment = new Experiment(
                "geo-exp",
                Environment.PROD,
                "",
                ExperimentStatus.ACTIVE,
                Collections.singletonList(rule),
                null,
                1L,
                1L,
                1L
        );
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("country", "IN");

        Optional<EvaluationDecision> decision = ruleEvaluationService.evaluateTargeting(experiment, attributes);
        assertTrue(decision.isPresent());
        assertTrue(decision.get().isInExperiment());
        assertEquals(EvaluationReason.TARGETING_MATCH, decision.get().getReason());
    }

    @Test
    void noMatchWhenAttributeMissing() {
        TargetingRule rule = new TargetingRule(
                "r1",
                1,
                Collections.singletonList(new Condition("country", ComparisonOperator.EQ, "IN")),
                true
        );
        Experiment experiment = new Experiment(
                "geo-exp",
                Environment.PROD,
                "",
                ExperimentStatus.ACTIVE,
                Collections.singletonList(rule),
                null,
                1L,
                1L,
                1L
        );

        Optional<EvaluationDecision> decision = ruleEvaluationService.evaluateTargeting(experiment, Collections.<String, Object>emptyMap());
        assertFalse(decision.isPresent());
    }

    @Test
    void lowerPriorityEvaluatedFirst() {
        TargetingRule highPriority = new TargetingRule(
                "high",
                10,
                Collections.singletonList(new Condition("tier", ComparisonOperator.EQ, "gold")),
                true
        );
        TargetingRule lowPriority = new TargetingRule(
                "low",
                1,
                Collections.singletonList(new Condition("tier", ComparisonOperator.EQ, "silver")),
                true
        );
        Experiment experiment = new Experiment(
                "priority-exp",
                Environment.PROD,
                "",
                ExperimentStatus.ACTIVE,
                Arrays.asList(highPriority, lowPriority),
                null,
                1L,
                1L,
                1L
        );
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("tier", "silver");

        Optional<EvaluationDecision> decision = ruleEvaluationService.evaluateTargeting(experiment, attributes);
        assertTrue(decision.isPresent());
        assertEquals("low", decision.get().getMatchedRuleId());
    }
}
