package com.razorpay.experiment.repository;

import com.razorpay.experiment.enums.Environment;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.mapper.ExperimentJsonMapper;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.RolloutConfig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ExperimentRowMapper {

    private final ExperimentJsonMapper jsonMapper;

    public ExperimentRowMapper(ExperimentJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Experiment mapRow(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        Environment environment = Environment.valueOf(rs.getString("environment"));
        String description = rs.getString("description");
        ExperimentStatus status = ExperimentStatus.valueOf(rs.getString("status"));
        String targetingJson = rs.getString("targeting_rules_json");
        String rolloutJson = rs.getString("rollout_json");
        long version = rs.getLong("version");
        long createdAt = rs.getLong("created_at");
        long updatedAt = rs.getLong("updated_at");

        List<com.razorpay.experiment.model.TargetingRule> rules = jsonMapper.fromTargetingRulesJson(targetingJson);
        RolloutConfig rollout = jsonMapper.fromRolloutJson(rolloutJson);

        return new Experiment(
                name,
                environment,
                description,
                status,
                rules,
                rollout,
                version,
                createdAt,
                updatedAt
        );
    }
}
