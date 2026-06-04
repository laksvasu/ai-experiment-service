package com.razorpay.experiment.mapper;

import com.razorpay.experiment.dto.ConditionDto;
import com.razorpay.experiment.dto.CreateExperimentRequest;
import com.razorpay.experiment.dto.ExperimentResponse;
import com.razorpay.experiment.dto.RolloutConfigDto;
import com.razorpay.experiment.dto.TargetingRuleDto;
import com.razorpay.experiment.dto.UpdateExperimentRequest;
import com.razorpay.experiment.enums.ExperimentStatus;
import com.razorpay.experiment.model.Condition;
import com.razorpay.experiment.model.Experiment;
import com.razorpay.experiment.model.RolloutConfig;
import com.razorpay.experiment.model.TargetingRule;

import java.util.ArrayList;
import java.util.List;

public class ExperimentMapper {

    public Experiment toModel(CreateExperimentRequest request, long version, long createdAt, long updatedAt) {
        return new Experiment(
                request.getName(),
                request.getEnvironment(),
                request.getDescription(),
                request.getStatus() != null ? request.getStatus() : ExperimentStatus.ACTIVE,
                toTargetingRules(request.getTargetingRules()),
                toRollout(request.getRollout()),
                version,
                createdAt,
                updatedAt
        );
    }

    public Experiment mergeUpdate(UpdateExperimentRequest request, Experiment existing, long updatedAt) {
        return new Experiment(
                request.getName(),
                request.getEnvironment(),
                request.getDescription() != null ? request.getDescription() : existing.getDescription(),
                request.getStatus() != null ? request.getStatus() : existing.getStatus(),
                toTargetingRules(request.getTargetingRules()),
                toRollout(request.getRollout()),
                existing.getVersion() + 1,
                existing.getCreatedAt(),
                updatedAt
        );
    }

    public ExperimentResponse toResponse(Experiment experiment) {
        ExperimentResponse response = new ExperimentResponse();
        response.setName(experiment.getName());
        response.setEnvironment(experiment.getEnvironment());
        response.setDescription(experiment.getDescription());
        response.setStatus(experiment.getStatus());
        response.setTargetingRules(toTargetingRuleDtos(experiment.getTargetingRules()));
        response.setRollout(toRolloutDto(experiment.getRollout()));
        response.setVersion(experiment.getVersion());
        response.setCreatedAt(experiment.getCreatedAt());
        response.setUpdatedAt(experiment.getUpdatedAt());
        return response;
    }

    private List<TargetingRule> toTargetingRules(List<TargetingRuleDto> dtos) {
        List<TargetingRule> rules = new ArrayList<TargetingRule>();
        if (dtos == null) {
            return rules;
        }
        for (TargetingRuleDto dto : dtos) {
            rules.add(toTargetingRule(dto));
        }
        return rules;
    }

    private TargetingRule toTargetingRule(TargetingRuleDto dto) {
        List<Condition> conditions = new ArrayList<Condition>();
        if (dto.getConditions() != null) {
            for (ConditionDto conditionDto : dto.getConditions()) {
                conditions.add(new Condition(
                        conditionDto.getAttribute(),
                        conditionDto.getOperator(),
                        conditionDto.getOperand()
                ));
            }
        }
        return new TargetingRule(
                dto.getId(),
                dto.getPriority(),
                conditions,
                dto.isMatchInExperiment()
        );
    }

    private RolloutConfig toRollout(RolloutConfigDto dto) {
        if (dto == null) {
            return null;
        }
        return new RolloutConfig(
                dto.getPercentage(),
                dto.getBucketingSalt(),
                dto.isShareBucketAcrossExperiments()
        );
    }

    private List<TargetingRuleDto> toTargetingRuleDtos(List<TargetingRule> rules) {
        List<TargetingRuleDto> dtos = new ArrayList<TargetingRuleDto>();
        for (TargetingRule rule : rules) {
            TargetingRuleDto dto = new TargetingRuleDto();
            dto.setId(rule.getId());
            dto.setPriority(rule.getPriority());
            dto.setMatchInExperiment(rule.isMatchInExperiment());
            List<ConditionDto> conditionDtos = new ArrayList<ConditionDto>();
            for (Condition condition : rule.getConditions()) {
                ConditionDto conditionDto = new ConditionDto();
                conditionDto.setAttribute(condition.getAttribute());
                conditionDto.setOperator(condition.getOperator());
                conditionDto.setOperand(condition.getOperand());
                conditionDtos.add(conditionDto);
            }
            dto.setConditions(conditionDtos);
            dtos.add(dto);
        }
        return dtos;
    }

    private RolloutConfigDto toRolloutDto(RolloutConfig rollout) {
        if (rollout == null) {
            return null;
        }
        RolloutConfigDto dto = new RolloutConfigDto();
        dto.setPercentage(rollout.getPercentage());
        dto.setBucketingSalt(rollout.getBucketingSalt());
        dto.setShareBucketAcrossExperiments(rollout.isShareBucketAcrossExperiments());
        return dto;
    }
}
