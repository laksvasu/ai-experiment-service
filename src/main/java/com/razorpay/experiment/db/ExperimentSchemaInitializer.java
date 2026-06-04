package com.razorpay.experiment.db;

import com.razorpay.experiment.exception.ExperimentPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class ExperimentSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ExperimentSchemaInitializer.class);

    private final DataSource dataSource;

    public ExperimentSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        String ddl = loadSchema();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : ddl.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            log.info("Experiment schema initialized");
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to initialize schema", e);
        }
    }

    private String loadSchema() {
        InputStream stream = ExperimentSchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql");
        if (stream == null) {
            throw new ExperimentPersistenceException("schema.sql not found on classpath", null);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new ExperimentPersistenceException("Failed to read schema.sql", e);
        }
    }
}
