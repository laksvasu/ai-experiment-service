package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.exception.InvalidExperimentConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperimentValidatorTest {

    private final ExperimentValidator validator = new ExperimentValidator();

    @Test
    void rejectsInvalidRolloutPercentage() {
        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setName("bad-exp");
        request.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(150);
        request.setRollout(rollout);

        assertThrows(InvalidExperimentConfigException.class, () -> validator.validateCreate(request));
    }

    @Test
    void rejectsExperimentWithoutRulesOrRollout() {
        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setName("empty-exp");
        request.setEnvironment(Environment.PROD);

        assertThrows(InvalidExperimentConfigException.class, () -> validator.validateCreate(request));
    }
}
