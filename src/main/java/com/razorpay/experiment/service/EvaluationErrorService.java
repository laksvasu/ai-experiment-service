package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.enums.EvaluationReason;

public interface EvaluationErrorService {

    EvaluateExperimentResponse failSafe(EvaluateExperimentRequest request, Throwable error, long snapshotVersion);
}
