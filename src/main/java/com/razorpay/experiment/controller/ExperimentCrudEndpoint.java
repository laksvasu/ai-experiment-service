package com.razorpay.experiment.controller;

import com.razorpay.experiment.api.ExperimentCrudApi;
import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.DeleteExperimentResponse;
import com.razorpay.experiment.dto.ExperimentResponse;
import com.razorpay.experiment.dto.ListExperimentsResponse;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.service.ExperimentCrudService;

/**
 * User-facing CRUD endpoints only — delegates to {@link ExperimentCrudService}.
 * <ul>
 *   <li>POST   /experiments</li>
 *   <li>GET    /experiments/{name}</li>
 *   <li>PUT    /experiments/{name}</li>
 *   <li>DELETE /experiments/{name}</li>
 *   <li>GET    /experiments</li>
 * </ul>
 */
public class ExperimentCrudEndpoint implements ExperimentCrudApi {

    private final ExperimentCrudService experimentCrudService;

    public ExperimentCrudEndpoint(ExperimentCrudService experimentCrudService) {
        this.experimentCrudService = experimentCrudService;
    }

    /** POST /experiments */
    @Override
    public ExperimentResponse createExperiment(CreateExperimentRequest request) {
        return experimentCrudService.createExperiment(request);
    }

    /** GET /experiments/{name} */
    @Override
    public ExperimentResponse getExperiment(String name, Environment environment) {
        return experimentCrudService.getExperiment(name, environment);
    }

    /** PUT /experiments/{name} */
    @Override
    public ExperimentResponse updateExperiment(UpdateExperimentRequest request) {
        return experimentCrudService.updateExperiment(request);
    }

    /** DELETE /experiments/{name} */
    @Override
    public DeleteExperimentResponse deleteExperiment(String name, Environment environment) {
        return experimentCrudService.deleteExperiment(name, environment);
    }

    /** GET /experiments */
    @Override
    public ListExperimentsResponse listExperiments(Environment environment) {
        return experimentCrudService.listExperiments(environment);
    }
}
