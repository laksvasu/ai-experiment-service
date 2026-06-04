package com.razorpay.experiment.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TargetingRule {

    private final String id;
    private final int priority;
    private final List<Condition> conditions;
    private final boolean matchInExperiment;

    public TargetingRule(String id, int priority, List<Condition> conditions, boolean matchInExperiment) {
        this.id = id;
        this.priority = priority;
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.matchInExperiment = matchInExperiment;
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public boolean isMatchInExperiment() {
        return matchInExperiment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TargetingRule that = (TargetingRule) o;
        return priority == that.priority
                && matchInExperiment == that.matchInExperiment
                && Objects.equals(id, that.id)
                && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, priority, conditions, matchInExperiment);
    }
}
