package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.exception.InvalidExperimentConfigException;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.RolloutConfig;

public class ExperimentValidator {

    public void validateCreate(CreateExperimentRequest request) {
        if (request == null) {
            throw new InvalidExperimentConfigException("Create request must not be null");
        }
        validateName(request.getName());
        if (request.getEnvironment() == null) {
            throw new InvalidExperimentConfigException("Environment is required");
        }
        validateExperimentConfig(request.getRollout(), request.getTargetingRules() != null && !request.getTargetingRules().isEmpty());
    }

    public void validateUpdate(UpdateExperimentRequest request) {
        if (request == null) {
            throw new InvalidExperimentConfigException("Update request must not be null");
        }
        validateName(request.getName());
        if (request.getEnvironment() == null) {
            throw new InvalidExperimentConfigException("Environment is required");
        }
        if (request.getVersion() < 1) {
            throw new InvalidExperimentConfigException("Version must be >= 1");
        }
        validateExperimentConfig(request.getRollout(), request.getTargetingRules() != null && !request.getTargetingRules().isEmpty());
    }

    public void validateModel(Experiment experiment) {
        validateName(experiment.getName());
        if (experiment.getEnvironment() == null) {
            throw new InvalidExperimentConfigException("Environment is required");
        }
        validateRollout(experiment.getRollout());
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidExperimentConfigException("Experiment name must not be blank");
        }
    }

    private void validateExperimentConfig(RolloutConfigDto rollout, boolean hasRules) {
        if (rollout == null && !hasRules) {
            throw new InvalidExperimentConfigException("Experiment must have targeting rules and/or rollout configuration");
        }
        if (rollout != null) {
            validateRolloutPercentage(rollout.getPercentage());
        }
    }

    private void validateRollout(RolloutConfig rollout) {
        if (rollout != null) {
            validateRolloutPercentage(rollout.getPercentage());
        }
    }

    private void validateRolloutPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new InvalidExperimentConfigException("Rollout percentage must be between 0 and 100");
        }
    }
}
