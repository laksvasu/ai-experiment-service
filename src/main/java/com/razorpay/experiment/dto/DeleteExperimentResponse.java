package com.razorpay.experiment.dto;

import com.razorpay.experiment.enums.Environment;

public class DeleteExperimentResponse {

    private boolean deleted;
    private String name;
    private Environment environment;

    public DeleteExperimentResponse() {
    }

    public DeleteExperimentResponse(boolean deleted, String name, Environment environment) {
        this.deleted = deleted;
        this.name = name;
        this.environment = environment;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
