package com.razorpay.experiment.service;

import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;

import java.util.Map;
import java.util.Optional;

public interface RuleEvaluationService {

    Optional<EvaluationDecision> evaluateTargeting(Experiment experiment, Map<String, Object> attributes);
}
