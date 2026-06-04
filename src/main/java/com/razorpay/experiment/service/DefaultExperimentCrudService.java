package com.razorpay.experiment.service;

import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.DeleteExperimentResponse;
import com.razorpay.experiment.dto.ExperimentResponse;
import com.razorpay.experiment.dto.ListExperimentsResponse;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.exception.ExperimentAlreadyExistsException;
import com.razorpay.experiment.exception.ExperimentNotFoundException;
import com.razorpay.experiment.exception.VersionConflictException;
import com.razorpay.experiment.mapper.ExperimentMapper;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.repository.ExperimentRepository;

import java.util.ArrayList;
import java.util.List;

public class DefaultExperimentCrudService implements ExperimentCrudService {

    private final ExperimentRepository experimentRepository;
    private final SnapshotService snapshotService;
    private final ExperimentMapper experimentMapper;
    private final ExperimentValidator experimentValidator;

    public DefaultExperimentCrudService(ExperimentRepository experimentRepository,
                                        SnapshotService snapshotService,
                                        ExperimentMapper experimentMapper,
                                        ExperimentValidator experimentValidator) {
        this.experimentRepository = experimentRepository;
        this.snapshotService = snapshotService;
        this.experimentMapper = experimentMapper;
        this.experimentValidator = experimentValidator;
    }

    @Override
    public ExperimentResponse createExperiment(CreateExperimentRequest request) {
        experimentValidator.validateCreate(request);
        if (experimentRepository.exists(request.getName(), request.getEnvironment())) {
            throw new ExperimentAlreadyExistsException(
                    "Experiment already exists: " + request.getName() + " in " + request.getEnvironment());
        }
        long now = System.currentTimeMillis();
        Experiment experiment = experimentMapper.toModel(request, 1L, now, now);
        experimentValidator.validateModel(experiment);
        experimentRepository.insert(experiment);
        snapshotService.refresh();
        return experimentMapper.toResponse(experiment);
    }

    @Override
    public ExperimentResponse getExperiment(String name, Environment environment) {
        Experiment experiment = experimentRepository.findByNameAndEnvironment(name, environment)
                .orElseThrow(() -> new ExperimentNotFoundException(
                        "Experiment not found: " + name + " in " + environment));
        return experimentMapper.toResponse(experiment);
    }

    @Override
    public ExperimentResponse updateExperiment(UpdateExperimentRequest request) {
        experimentValidator.validateUpdate(request);
        Experiment existing = experimentRepository.findByNameAndEnvironment(request.getName(), request.getEnvironment())
                .orElseThrow(() -> new ExperimentNotFoundException(
                        "Experiment not found: " + request.getName() + " in " + request.getEnvironment()));

        if (existing.getVersion() != request.getVersion()) {
            throw new VersionConflictException(
                    "Version conflict for experiment " + request.getName()
                            + ": expected " + request.getVersion() + " but found " + existing.getVersion());
        }

        long now = System.currentTimeMillis();
        Experiment updated = experimentMapper.mergeUpdate(request, existing, now);
        experimentValidator.validateModel(updated);

        int rows = experimentRepository.update(updated, request.getVersion());
        if (rows == 0) {
            throw new VersionConflictException(
                    "Version conflict for experiment " + request.getName() + " during update");
        }
        snapshotService.refresh();
        return experimentMapper.toResponse(updated);
    }

    @Override
    public DeleteExperimentResponse deleteExperiment(String name, Environment environment) {
        boolean deleted = experimentRepository.delete(name, environment);
        if (deleted) {
            snapshotService.refresh();
        }
        return new DeleteExperimentResponse(deleted, name, environment);
    }

    @Override
    public ListExperimentsResponse listExperiments(Environment environment) {
        List<Experiment> experiments = environment == null
                ? experimentRepository.findAll()
                : experimentRepository.findByEnvironment(environment);
        ListExperimentsResponse response = new ListExperimentsResponse();
        List<ExperimentResponse> experimentResponses = new ArrayList<ExperimentResponse>();
        for (Experiment experiment : experiments) {
            experimentResponses.add(experimentMapper.toResponse(experiment));
        }
        response.setExperiments(experimentResponses);
        response.setTotal(experimentResponses.size());
        return response;
    }
}
