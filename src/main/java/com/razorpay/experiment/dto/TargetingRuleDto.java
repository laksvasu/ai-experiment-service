package com.razorpay.experiment.dto;

import java.util.ArrayList;
import java.util.List;

public class TargetingRuleDto {

    private String id;
    private int priority;
    private List<ConditionDto> conditions = new ArrayList<ConditionDto>();
    private boolean matchInExperiment = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<ConditionDto> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDto> conditions) {
        this.conditions = conditions;
    }

    public boolean isMatchInExperiment() {
        return matchInExperiment;
    }

    public void setMatchInExperiment(boolean matchInExperiment) {
        this.matchInExperiment = matchInExperiment;
    }
}
