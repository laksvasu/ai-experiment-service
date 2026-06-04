package com.razorpay.experiment;

import com.razorpay.experiment.config.ExperimentApplicationContext;
import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.enums.Environment;

/**
 * Minimal demo entry point for manual smoke testing.
 */
public final class ExperimentServiceDemo {

    private ExperimentServiceDemo() {
    }

    public static void main(String[] args) {
        ExperimentApplicationContext context = ExperimentApplicationContext.createDefault();
        try {
            CreateExperimentRequest create = new CreateExperimentRequest();
            create.setName("new-checkout");
            create.setEnvironment(Environment.PROD);
            RolloutConfigDto rollout = new RolloutConfigDto();
            rollout.setPercentage(20);
            rollout.setBucketingSalt("demo-salt");
            create.setRollout(rollout);
            context.getExperimentCrudApi().createExperiment(create);

            EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
            evaluate.setCustomerKey("customer-123");
            evaluate.setExperimentName("new-checkout");
            evaluate.setEnvironment(Environment.PROD);

            EvaluateExperimentResponse response = context.getExperimentEvaluateApi().evaluateExperiment(evaluate);
            System.out.println("inExperiment=" + response.isInExperiment()
                    + " reason=" + response.getReason()
                    + " snapshotVersion=" + response.getSnapshotVersion());
        } finally {
            context.shutdown();
        }
    }
}
