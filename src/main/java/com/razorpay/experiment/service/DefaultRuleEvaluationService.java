package com.razorpay.experiment.service;

import com.razorpay.experiment.enums.ComparisonOperator;
import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.model.Condition;
import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.TargetingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultRuleEvaluationService implements RuleEvaluationService {

    @Override
    public Optional<EvaluationDecision> evaluateTargeting(Experiment experiment, Map<String, Object> attributes) {
        if (!experiment.hasTargetingRules()) {
            return Optional.empty();
        }
        Map<String, Object> safeAttributes = attributes != null ? attributes : Collections.<String, Object>emptyMap();
        List<TargetingRule> sorted = new ArrayList<TargetingRule>(experiment.getTargetingRules());
        sorted.sort(Comparator.comparingInt(TargetingRule::getPriority));

        for (TargetingRule rule : sorted) {
            if (matchesAllConditions(rule, safeAttributes)) {
                boolean inExperiment = rule.isMatchInExperiment();
                EvaluationReason reason = inExperiment ? EvaluationReason.TARGETING_MATCH : EvaluationReason.DEFAULT_OUT;
                return Optional.of(new EvaluationDecision(inExperiment, reason, rule.getId(), null));
            }
        }
        return Optional.empty();
    }

    private boolean matchesAllConditions(TargetingRule rule, Map<String, Object> attributes) {
        for (Condition condition : rule.getConditions()) {
            if (!matchesCondition(condition, attributes)) {
                return false;
            }
        }
        return !rule.getConditions().isEmpty();
    }

    private boolean matchesCondition(Condition condition, Map<String, Object> attributes) {
        Object actual = attributes.get(condition.getAttribute());
        Object expected = condition.getOperand();
        ComparisonOperator operator = condition.getOperator();

        if (operator == null) {
            return false;
        }

        switch (operator) {
            case EQ:
                return objectsEqual(actual, expected);
            case NEQ:
                return !objectsEqual(actual, expected);
            case IN:
                return expected instanceof Collection && collectionContains((Collection<?>) expected, actual);
            case NOT_IN:
                return expected instanceof Collection && !collectionContains((Collection<?>) expected, actual);
            case GT:
                return compareNumbers(actual, expected) > 0;
            case GTE:
                return compareNumbers(actual, expected) >= 0;
            case LT:
                return compareNumbers(actual, expected) < 0;
            case LTE:
                return compareNumbers(actual, expected) <= 0;
            default:
                return false;
        }
    }

    private boolean objectsEqual(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        return String.valueOf(actual).equals(String.valueOf(expected));
    }

    private boolean collectionContains(Collection<?> collection, Object value) {
        for (Object item : collection) {
            if (objectsEqual(item, value)) {
                return true;
            }
        }
        return false;
    }

    private int compareNumbers(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return -1;
        }
        double a = Double.parseDouble(String.valueOf(actual));
        double b = Double.parseDouble(String.valueOf(expected));
        return Double.compare(a, b);
    }
}
