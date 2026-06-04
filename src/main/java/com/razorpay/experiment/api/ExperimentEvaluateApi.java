package com.razorpay.experiment.api;

import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;

/**
 * Public contract for runtime experiment enrollment (host application entry).
 */
public interface ExperimentEvaluateApi {

    EvaluateExperimentResponse evaluateExperiment(EvaluateExperimentRequest request);
}
