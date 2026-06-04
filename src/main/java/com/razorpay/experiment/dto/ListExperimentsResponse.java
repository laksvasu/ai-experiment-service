package com.razorpay.experiment.dto;

import java.util.ArrayList;
import java.util.List;

public class ListExperimentsResponse {

    private List<ExperimentResponse> experiments = new ArrayList<ExperimentResponse>();
    private int total;

    public List<ExperimentResponse> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentResponse> experiments) {
        this.experiments = experiments;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
