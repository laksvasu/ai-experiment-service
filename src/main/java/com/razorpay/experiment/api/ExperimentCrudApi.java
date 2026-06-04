package com.razorpay.experiment.api;

import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.DeleteExperimentResponse;
import com.razorpay.experiment.dto.ExperimentResponse;
import com.razorpay.experiment.dto.ListExperimentsResponse;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.enums.Environment;

/**
 * Public contract for experiment CRUD (host application entry).
 */
public interface ExperimentCrudApi {

    ExperimentResponse createExperiment(CreateExperimentRequest request);

    ExperimentResponse getExperiment(String name, Environment environment);

    ExperimentResponse updateExperiment(UpdateExperimentRequest request);

    DeleteExperimentResponse deleteExperiment(String name, Environment environment);

    ListExperimentsResponse listExperiments(Environment environment);
}
