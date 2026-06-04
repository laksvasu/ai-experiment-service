package com.razorpay.experiment;

import com.razorpay.experiment.api.ExperimentCrudApi;
import com.razorpay.experiment.api.ExperimentEvaluateApi;
import com.razorpay.experiment.config.ExperimentApplicationContext;
import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentResponse;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.EvaluationReason;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.exception.ExperimentNotFoundException;
import com.razorpay.experiment.exception.InvalidExperimentConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentUserFlowIT {

    private ExperimentApplicationContext context;
    private ExperimentCrudApi experimentCrudApi;
    private ExperimentEvaluateApi experimentEvaluateApi;

    @BeforeEach
    void setUp() {
        String dbName = "test_" + UUID.randomUUID().toString().replace("-", "");
        context = ExperimentApplicationContext.create(
                "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
        experimentCrudApi = context.getExperimentCrudApi();
        experimentEvaluateApi = context.getExperimentEvaluateApi();
    }

    @AfterEach
    void tearDown() {
        context.shutdown();
    }

    @Test
    void createEvaluateAndDeleteRolloutExperiment() {
        String experimentName = "new-checkout";
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName(experimentName);
        create.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(100);
        rollout.setBucketingSalt("test-salt");
        create.setRollout(rollout);

        experimentCrudApi.createExperiment(create);

        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("user-123");
        evaluate.setExperimentName(experimentName);
        evaluate.setEnvironment(Environment.PROD);

        EvaluateExperimentResponse response = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertTrue(response.isInExperiment());
        assertEquals(EvaluationReason.ROLLOUT_IN, response.getReason());
        assertTrue(response.getSnapshotVersion() > 0);

        EvaluateExperimentResponse repeat = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertEquals(response.isInExperiment(), repeat.isInExperiment());

        assertTrue(experimentCrudApi.deleteExperiment(experimentName, Environment.PROD).isDeleted());

        EvaluateExperimentResponse afterDelete = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertFalse(afterDelete.isInExperiment());
        assertEquals(EvaluationReason.NOT_FOUND, afterDelete.getReason());
    }

    @Test
    void pausedExperimentReturnsFalse() {
        String experimentName = "paused-exp";
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName(experimentName);
        create.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(100);
        create.setRollout(rollout);
        experimentCrudApi.createExperiment(create);

        UpdateExperimentRequest update = new UpdateExperimentRequest();
        update.setName(experimentName);
        update.setEnvironment(Environment.PROD);
        update.setStatus(ExperimentStatus.PAUSED);
        update.setVersion(1L);
        update.setRollout(rollout);
        experimentCrudApi.updateExperiment(update);

        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("user-1");
        evaluate.setExperimentName(experimentName);
        evaluate.setEnvironment(Environment.PROD);

        EvaluateExperimentResponse response = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertFalse(response.isInExperiment());
        assertEquals(EvaluationReason.PAUSED, response.getReason());
    }

    @Test
    void unknownExperimentReturnsNotFoundWithoutThrowing() {
        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("user-1");
        evaluate.setExperimentName("missing");
        evaluate.setEnvironment(Environment.PROD);

        EvaluateExperimentResponse response = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertFalse(response.isInExperiment());
        assertEquals(EvaluationReason.NOT_FOUND, response.getReason());
    }

    @Test
    void invalidCreateIsRejected() {
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName("invalid");
        create.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(200);
        create.setRollout(rollout);

        assertThrows(InvalidExperimentConfigException.class, () -> experimentCrudApi.createExperiment(create));
    }

    @Test
    void environmentIsolation() {
        String name = "shared-name";
        RolloutConfigDto devRollout = new RolloutConfigDto();
        devRollout.setPercentage(0);
        RolloutConfigDto prodRollout = new RolloutConfigDto();
        prodRollout.setPercentage(100);

        CreateExperimentRequest dev = new CreateExperimentRequest();
        dev.setName(name);
        dev.setEnvironment(Environment.DEV);
        dev.setRollout(devRollout);

        CreateExperimentRequest prod = new CreateExperimentRequest();
        prod.setName(name);
        prod.setEnvironment(Environment.PROD);
        prod.setRollout(prodRollout);

        experimentCrudApi.createExperiment(dev);
        experimentCrudApi.createExperiment(prod);

        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("user-env");
        evaluate.setExperimentName(name);

        evaluate.setEnvironment(Environment.DEV);
        assertFalse(experimentEvaluateApi.evaluateExperiment(evaluate).isInExperiment());

        evaluate.setEnvironment(Environment.PROD);
        assertTrue(experimentEvaluateApi.evaluateExperiment(evaluate).isInExperiment());
    }

    @Test
    void getExistingExperiment() {
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName("readable");
        create.setEnvironment(Environment.STAGING);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(10);
        create.setRollout(rollout);
        experimentCrudApi.createExperiment(create);

        assertEquals("readable", experimentCrudApi.getExperiment("readable", Environment.STAGING).getName());
        assertThrows(ExperimentNotFoundException.class,
                () -> experimentCrudApi.getExperiment("readable", Environment.PROD));
    }

    @Test
    void evaluateWithBlankCustomerKeyFailsSafe() {
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName("blank-key-exp");
        create.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(100);
        create.setRollout(rollout);
        experimentCrudApi.createExperiment(create);

        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("");
        evaluate.setExperimentName("blank-key-exp");
        evaluate.setEnvironment(Environment.PROD);

        EvaluateExperimentResponse response = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertFalse(response.isInExperiment());
        assertEquals(EvaluationReason.ERROR_DEFAULT, response.getReason());
    }

    @Test
    void targetingRuleMatchesBeforeRollout() {
        String name = "geo-exp";
        CreateExperimentRequest create = new CreateExperimentRequest();
        create.setName(name);
        create.setEnvironment(Environment.PROD);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(0);
        create.setRollout(rollout);

        com.razorpay.experiment.dto.TargetingRuleDto rule = new com.razorpay.experiment.dto.TargetingRuleDto();
        rule.setId("india");
        rule.setPriority(1);
        com.razorpay.experiment.dto.ConditionDto condition = new com.razorpay.experiment.dto.ConditionDto();
        condition.setAttribute("country");
        condition.setOperator(com.razorpay.experiment.enums.ComparisonOperator.EQ);
        condition.setOperand("IN");
        rule.setConditions(java.util.Collections.singletonList(condition));
        create.setTargetingRules(java.util.Collections.singletonList(rule));

        experimentCrudApi.createExperiment(create);

        EvaluateExperimentRequest evaluate = new EvaluateExperimentRequest();
        evaluate.setCustomerKey("user-in");
        evaluate.setExperimentName(name);
        evaluate.setEnvironment(Environment.PROD);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("country", "IN");
        evaluate.setAttributes(attributes);

        EvaluateExperimentResponse response = experimentEvaluateApi.evaluateExperiment(evaluate);
        assertTrue(response.isInExperiment());
        assertEquals(EvaluationReason.TARGETING_MATCH, response.getReason());
    }
}
