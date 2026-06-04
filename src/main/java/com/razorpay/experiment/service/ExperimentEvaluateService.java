package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;

public interface ExperimentEvaluateService {

    /**
     * Evaluates enrollment; never throws to the caller (fail-safe defaults).
     */
    EvaluateExperimentResponse evaluateExperiment(EvaluateExperimentRequest request);
}
