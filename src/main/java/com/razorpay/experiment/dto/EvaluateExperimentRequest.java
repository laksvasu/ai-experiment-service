package com.razorpay.experiment.dto;

import com.razorpay.experiment.enums.Environment;

import java.util.HashMap;
import java.util.Map;

public class EvaluateExperimentRequest {

    private String customerKey;
    private String experimentName;
    private Environment environment;
    private Map<String, Object> attributes = new HashMap<String, Object>();

    public String getCustomerKey() {
        return customerKey;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
