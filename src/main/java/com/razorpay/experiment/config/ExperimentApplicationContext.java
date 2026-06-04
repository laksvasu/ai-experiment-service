package com.razorpay.experiment.config;

import com.razorpay.experiment.api.ExperimentCrudApi;
import com.razorpay.experiment.api.ExperimentEvaluateApi;
import com.razorpay.experiment.controller.ExperimentCrudEndpoint;
import com.razorpay.experiment.controller.ExperimentEvaluateEndpoint;
import com.razorpay.experiment.db.ExperimentSchemaInitializer;
import com.razorpay.experiment.db.H2DataSourceConfig;
import com.razorpay.experiment.mapper.ExperimentJsonMapper;
import com.razorpay.experiment.mapper.ExperimentMapper;
import com.razorpay.experiment.repository.AtomicSnapshotRepository;
import com.razorpay.experiment.repository.ExperimentRepository;
import com.razorpay.experiment.repository.ExperimentRowMapper;
import com.razorpay.experiment.repository.H2ExperimentRepository;
import com.razorpay.experiment.repository.SnapshotRepository;
import com.razorpay.experiment.service.DefaultExperimentCrudService;
import com.razorpay.experiment.service.DefaultExperimentEvaluateService;
import com.razorpay.experiment.service.DefaultRolloutService;
import com.razorpay.experiment.service.DefaultRuleEvaluationService;
import com.razorpay.experiment.service.DefaultSnapshotService;
import com.razorpay.experiment.service.EvaluationErrorService;
import com.razorpay.experiment.service.ExperimentCrudService;
import com.razorpay.experiment.service.ExperimentEvaluateService;
import com.razorpay.experiment.service.ExperimentValidator;
import com.razorpay.experiment.service.LoggingEvaluationErrorService;
import com.razorpay.experiment.service.RolloutService;
import com.razorpay.experiment.service.RuleEvaluationService;
import com.razorpay.experiment.service.SnapshotService;

import javax.sql.DataSource;

public final class ExperimentApplicationContext {

    private static final String DEFAULT_JDBC_URL =
            "jdbc:h2:mem:experiments;DB_CLOSE_DELAY=-1;MODE=MySQL";

    private final H2DataSourceConfig dataSourceConfig;
    private final ExperimentCrudApi experimentCrudApi;
    private final ExperimentEvaluateApi experimentEvaluateApi;

    private ExperimentApplicationContext(H2DataSourceConfig dataSourceConfig,
                                           ExperimentCrudApi experimentCrudApi,
                                           ExperimentEvaluateApi experimentEvaluateApi) {
        this.dataSourceConfig = dataSourceConfig;
        this.experimentCrudApi = experimentCrudApi;
        this.experimentEvaluateApi = experimentEvaluateApi;
    }

    public static ExperimentApplicationContext createDefault() {
        return create(DEFAULT_JDBC_URL);
    }

    public static ExperimentApplicationContext create(String jdbcUrl) {
        H2DataSourceConfig dataSourceConfig = new H2DataSourceConfig(jdbcUrl);
        DataSource dataSource = dataSourceConfig.getDataSource();

        new ExperimentSchemaInitializer(dataSource).initialize();

        ExperimentJsonMapper jsonMapper = new ExperimentJsonMapper();
        ExperimentRowMapper rowMapper = new ExperimentRowMapper(jsonMapper);
        ExperimentRepository experimentRepository = new H2ExperimentRepository(dataSource, rowMapper, jsonMapper);
        SnapshotRepository snapshotRepository = new AtomicSnapshotRepository();

        SnapshotService snapshotService = new DefaultSnapshotService(experimentRepository, snapshotRepository);
        snapshotService.init();

        ExperimentMapper experimentMapper = new ExperimentMapper();
        ExperimentValidator experimentValidator = new ExperimentValidator();
        ExperimentCrudService experimentCrudService = new DefaultExperimentCrudService(
                experimentRepository, snapshotService, experimentMapper, experimentValidator);

        RuleEvaluationService ruleEvaluationService = new DefaultRuleEvaluationService();
        RolloutService rolloutService = new DefaultRolloutService();
        EvaluationErrorService evaluationErrorService = new LoggingEvaluationErrorService();
        ExperimentEvaluateService experimentEvaluateService = new DefaultExperimentEvaluateService(
                snapshotService, ruleEvaluationService, rolloutService, evaluationErrorService);

        ExperimentCrudApi crudApi = new ExperimentCrudEndpoint(experimentCrudService);
        ExperimentEvaluateApi evaluateApi = new ExperimentEvaluateEndpoint(experimentEvaluateService);

        return new ExperimentApplicationContext(dataSourceConfig, crudApi, evaluateApi);
    }

    public ExperimentCrudApi getExperimentCrudApi() {
        return experimentCrudApi;
    }

    public ExperimentEvaluateApi getExperimentEvaluateApi() {
        return experimentEvaluateApi;
    }

    public void shutdown() {
        dataSourceConfig.shutdown();
    }
}
