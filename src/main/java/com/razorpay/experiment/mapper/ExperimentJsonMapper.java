package com.razorpay.experiment.mapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.razorpay.experiment.model.RolloutConfig;
import com.razorpay.experiment.model.TargetingRule;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class ExperimentJsonMapper {

    private static final Type TARGETING_RULE_LIST_TYPE = new TypeToken<List<TargetingRule>>() {
    }.getType();

    private final Gson gson;

    public ExperimentJsonMapper() {
        this.gson = new GsonBuilder().create();
    }

    public String toTargetingRulesJson(List<TargetingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return gson.toJson(rules);
    }

    public List<TargetingRule> fromTargetingRulesJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<TargetingRule> rules = gson.fromJson(json, TARGETING_RULE_LIST_TYPE);
        return rules != null ? rules : Collections.<TargetingRule>emptyList();
    }

    public String toRolloutJson(RolloutConfig rollout) {
        if (rollout == null) {
            return null;
        }
        return gson.toJson(rollout);
    }

    public RolloutConfig fromRolloutJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return gson.fromJson(json, RolloutConfig.class);
    }
}
