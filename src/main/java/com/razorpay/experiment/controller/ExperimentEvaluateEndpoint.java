package com.razorpay.experiment.controller;

import com.razorpay.experiment.api.ExperimentEvaluateApi;
import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.service.ExperimentEvaluateService;

/**
 * User-facing runtime endpoint only — delegates to {@link ExperimentEvaluateService}.
 * <ul>
 *   <li>POST /experiments/evaluate</li>
 * </ul>
 */
public class ExperimentEvaluateEndpoint implements ExperimentEvaluateApi {

    private final ExperimentEvaluateService experimentEvaluateService;

    public ExperimentEvaluateEndpoint(ExperimentEvaluateService experimentEvaluateService) {
        this.experimentEvaluateService = experimentEvaluateService;
    }

    /** POST /experiments/evaluate */
    @Override
    public EvaluateExperimentResponse evaluateExperiment(EvaluateExperimentRequest request) {
        return experimentEvaluateService.evaluateExperiment(request);
    }
}
