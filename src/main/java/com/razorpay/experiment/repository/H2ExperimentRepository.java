package com.razorpay.experiment.repository;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.exception.ExperimentPersistenceException;
import com.razorpay.experiment.mapper.ExperimentJsonMapper;
import com.razorpay.experiment.model.Experiment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2ExperimentRepository implements ExperimentRepository {

    private static final String INSERT_SQL =
            "INSERT INTO experiments (name, environment, description, status, targeting_rules_json, "
                    + "rollout_json, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE experiments SET description = ?, status = ?, targeting_rules_json = ?, rollout_json = ?, "
                    + "version = ?, updated_at = ? WHERE name = ? AND environment = ? AND version = ?";

    private static final String SELECT_ONE_SQL =
            "SELECT * FROM experiments WHERE name = ? AND environment = ?";

    private static final String SELECT_ALL_SQL = "SELECT * FROM experiments";

    private static final String SELECT_BY_ENV_SQL =
            "SELECT * FROM experiments WHERE environment = ?";

    private static final String DELETE_SQL =
            "DELETE FROM experiments WHERE name = ? AND environment = ?";

    private static final String EXISTS_SQL =
            "SELECT 1 FROM experiments WHERE name = ? AND environment = ?";

    private final DataSource dataSource;
    private final ExperimentRowMapper rowMapper;
    private final ExperimentJsonMapper jsonMapper;

    public H2ExperimentRepository(DataSource dataSource,
                                  ExperimentRowMapper rowMapper,
                                  ExperimentJsonMapper jsonMapper) {
        this.dataSource = dataSource;
        this.rowMapper = rowMapper;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void insert(Experiment experiment) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            bindExperiment(ps, experiment);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to insert experiment: " + experiment.getName(), e);
        }
    }

    @Override
    public int update(Experiment experiment, long expectedVersion) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, experiment.getDescription());
            ps.setString(2, experiment.getStatus().name());
            ps.setString(3, jsonMapper.toTargetingRulesJson(experiment.getTargetingRules()));
            ps.setString(4, jsonMapper.toRolloutJson(experiment.getRollout()));
            ps.setLong(5, experiment.getVersion());
            ps.setLong(6, experiment.getUpdatedAt());
            ps.setString(7, experiment.getName());
            ps.setString(8, experiment.getEnvironment().name());
            ps.setLong(9, expectedVersion);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to update experiment: " + experiment.getName(), e);
        }
    }

    @Override
    public Optional<Experiment> findByNameAndEnvironment(String name, Environment environment) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ONE_SQL)) {
            ps.setString(1, name);
            ps.setString(2, environment.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to find experiment: " + name, e);
        }
    }

    @Override
    public List<Experiment> findAll() {
        return queryList(SELECT_ALL_SQL, null);
    }

    @Override
    public List<Experiment> findByEnvironment(Environment environment) {
        return queryList(SELECT_BY_ENV_SQL, environment);
    }

    @Override
    public boolean delete(String name, Environment environment) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_SQL)) {
            ps.setString(1, name);
            ps.setString(2, environment.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to delete experiment: " + name, e);
        }
    }

    @Override
    public boolean exists(String name, Environment environment) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(EXISTS_SQL)) {
            ps.setString(1, name);
            ps.setString(2, environment.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to check experiment existence: " + name, e);
        }
    }

    private List<Experiment> queryList(String sql, Environment environment) {
        List<Experiment> results = new ArrayList<Experiment>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (environment != null) {
                ps.setString(1, environment.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new ExperimentPersistenceException("Failed to query experiments", e);
        }
        return results;
    }

    private void bindExperiment(PreparedStatement ps, Experiment experiment) throws SQLException {
        ps.setString(1, experiment.getName());
        ps.setString(2, experiment.getEnvironment().name());
        ps.setString(3, experiment.getDescription());
        ps.setString(4, experiment.getStatus().name());
        ps.setString(5, jsonMapper.toTargetingRulesJson(experiment.getTargetingRules()));
        ps.setString(6, jsonMapper.toRolloutJson(experiment.getRollout()));
        ps.setLong(7, experiment.getVersion());
        ps.setLong(8, experiment.getCreatedAt());
        ps.setLong(9, experiment.getUpdatedAt());
    }
}
