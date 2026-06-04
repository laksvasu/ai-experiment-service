package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.enums.EvaluationReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEvaluationErrorService implements EvaluationErrorService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEvaluationErrorService.class);

    @Override
    public EvaluateExperimentResponse failSafe(EvaluateExperimentRequest request, Throwable error, long snapshotVersion) {
        log.error("event=experiment_eval_error experiment={} environment={} errorClass={} message={} snapshotVersion={}",
                request != null ? request.getExperimentName() : null,
                request != null ? request.getEnvironment() : null,
                error.getClass().getSimpleName(),
                error.getMessage(),
                snapshotVersion,
                error);
        String experimentName = request != null ? request.getExperimentName() : null;
        return new EvaluateExperimentResponse(false, experimentName, EvaluationReason.ERROR_DEFAULT, snapshotVersion);
    }
}
