package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.model.EvaluationDecision;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.ExperimentSnapshot;

import java.util.Optional;

public class DefaultExperimentEvaluateService implements ExperimentEvaluateService {

    private final SnapshotService snapshotService;
    private final RuleEvaluationService ruleEvaluationService;
    private final RolloutService rolloutService;
    private final EvaluationErrorService evaluationErrorService;

    public DefaultExperimentEvaluateService(SnapshotService snapshotService,
                                            RuleEvaluationService ruleEvaluationService,
                                            RolloutService rolloutService,
                                            EvaluationErrorService evaluationErrorService) {
        this.snapshotService = snapshotService;
        this.ruleEvaluationService = ruleEvaluationService;
        this.rolloutService = rolloutService;
        this.evaluationErrorService = evaluationErrorService;
    }

    @Override
    public EvaluateExperimentResponse evaluateExperiment(EvaluateExperimentRequest request) {
        long snapshotVersion = snapshotService.currentVersion();
        try {
            return doEvaluate(request);
        } catch (IllegalArgumentException e) {
            return evaluationErrorService.failSafe(request, e, snapshotVersion);
        } catch (RuntimeException e) {
            return evaluationErrorService.failSafe(request, e, snapshotVersion);
        }
    }

    private EvaluateExperimentResponse doEvaluate(EvaluateExperimentRequest request) {
        validateRequest(request);
        ExperimentSnapshot snapshot = snapshotService.getCurrentSnapshot();
        Optional<Experiment> experimentOpt = snapshot.find(
                request.getEnvironment(),
                request.getExperimentName()
        );

        if (!experimentOpt.isPresent()) {
            return new EvaluateExperimentResponse(
                    false,
                    request.getExperimentName(),
                    EvaluationReason.NOT_FOUND,
                    snapshot.getSnapshotVersion()
            );
        }

        Experiment experiment = experimentOpt.get();
        if(experiment.isKillSwitchEnabled()){
            return new EvaluateExperimentResponse(
                    false, request.getExperimentName(),
                    EvaluationReason.KILL,
                    snapshot.getSnapshotVersion()
            );
        }

        if (experiment.getStatus() == ExperimentStatus.PAUSED) {
            return new EvaluateExperimentResponse(
                    false,
                    request.getExperimentName(),
                    EvaluationReason.PAUSED,
                    snapshot.getSnapshotVersion()
            );
        }

        Optional<EvaluationDecision> targetingDecision = ruleEvaluationService.evaluateTargeting(
                experiment,
                request.getAttributes()
        );
        if (targetingDecision.isPresent()) {
            EvaluationDecision decision = targetingDecision.get();
            return new EvaluateExperimentResponse(
                    decision.isInExperiment(),
                    request.getExperimentName(),
                    decision.getReason(),
                    snapshot.getSnapshotVersion()
            );
        }

        if (experiment.hasRollout()) {
            EvaluationDecision rolloutDecision = rolloutService.evaluateRollout(experiment, request.getCustomerKey());
            return new EvaluateExperimentResponse(
                    rolloutDecision.isInExperiment(),
                    request.getExperimentName(),
                    rolloutDecision.getReason(),
                    snapshot.getSnapshotVersion()
            );
        }

        return new EvaluateExperimentResponse(
                false,
                request.getExperimentName(),
                EvaluationReason.DEFAULT_OUT,
                snapshot.getSnapshotVersion()
        );
    }

    private void validateRequest(EvaluateExperimentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Evaluate request must not be null");
        }
        if (request.getCustomerKey() == null || request.getCustomerKey().trim().isEmpty()) {
            throw new IllegalArgumentException("customerKey is required");
        }
        if (request.getExperimentName() == null || request.getExperimentName().trim().isEmpty()) {
            throw new IllegalArgumentException("experimentName is required");
        }
        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("environment is required");
        }
    }
}
