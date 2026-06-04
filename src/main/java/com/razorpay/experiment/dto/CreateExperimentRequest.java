package com.razorpay.experiment.dto;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.ExperimentStatus;

import java.util.ArrayList;
import java.util.List;

public class CreateExperimentRequest {

    private String name;
    private Environment environment;
    private String description;
    private ExperimentStatus status = ExperimentStatus.ACTIVE;
    private List<TargetingRuleDto> targetingRules = new ArrayList<TargetingRuleDto>();
    private RolloutConfigDto rollout;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public List<TargetingRuleDto> getTargetingRules() {
        return targetingRules;
    }

    public void setTargetingRules(List<TargetingRuleDto> targetingRules) {
        this.targetingRules = targetingRules;
    }

    public RolloutConfigDto getRollout() {
        return rollout;
    }

    public void setRollout(RolloutConfigDto rollout) {
        this.rollout = rollout;
    }
}
