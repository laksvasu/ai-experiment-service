package com.razorpay.experiment.repository;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.model.Experiment;

import java.util.List;
import java.util.Optional;

public interface ExperimentRepository {

    void insert(Experiment experiment);

    int update(Experiment experiment, long expectedVersion);

    Optional<Experiment> findByNameAndEnvironment(String name, Environment environment);

    List<Experiment> findAll();

    List<Experiment> findByEnvironment(Environment environment);

    boolean delete(String name, Environment environment);

    boolean exists(String name, Environment environment);
}
