package com.razorpay.experiment.service;

import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;

public interface RolloutService {

    EvaluationDecision evaluateRollout(Experiment experiment, String customerKey);
}
