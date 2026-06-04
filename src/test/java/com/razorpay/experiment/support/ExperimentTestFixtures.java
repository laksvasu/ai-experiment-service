package com.razorpay.experiment.support;

import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.EvaluateExperimentRequest;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.enums.ComparisonOperator;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.dto.ConditionDto;
import com.razorpay.experiment.dto.TargetingRuleDto;

import java.util.Collections;
import java.util.Map;

/**
 * Reusable builders for flow and integration tests.
 */
public final class ExperimentTestFixtures {

    private ExperimentTestFixtures() {
    }

    public static CreateExperimentRequest rolloutOnly(String name, Environment env, int percentage) {
        CreateExperimentRequest request = new CreateExperimentRequest();
        request.setName(name);
        request.setEnvironment(env);
        RolloutConfigDto rollout = new RolloutConfigDto();
        rollout.setPercentage(percentage);
        rollout.setBucketingSalt("test-salt");
        request.setRollout(rollout);
        return request;
    }

    public static CreateExperimentRequest withCountryRule(String name, Environment env, int rolloutPercentage) {
        CreateExperimentRequest request = rolloutOnly(name, env, rolloutPercentage);
        TargetingRuleDto rule = new TargetingRuleDto();
        rule.setId("country-in");
        rule.setPriority(1);
        ConditionDto condition = new ConditionDto();
        condition.setAttribute("country");
        condition.setOperator(ComparisonOperator.EQ);
        condition.setOperand("IN");
        rule.setConditions(Collections.singletonList(condition));
        request.setTargetingRules(Collections.singletonList(rule));
        return request;
    }

    public static EvaluateExperimentRequest evaluate(String experimentName,
                                                     Environment env,
                                                     String customerKey) {
        EvaluateExperimentRequest request = new EvaluateExperimentRequest();
        request.setExperimentName(experimentName);
        request.setEnvironment(env);
        request.setCustomerKey(customerKey);
        return request;
    }

    public static EvaluateExperimentRequest evaluate(String experimentName,
                                                     Environment env,
                                                     String customerKey,
                                                     Map<String, Object> attributes) {
        EvaluateExperimentRequest request = evaluate(experimentName, env, customerKey);
        request.setAttributes(attributes);
        return request;
    }
}
