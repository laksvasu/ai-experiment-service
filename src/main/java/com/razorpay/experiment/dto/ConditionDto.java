package com.razorpay.experiment.dto;

import com.razorpay.experiment.enums.ComparisonOperator;

public class ConditionDto {

    private String attribute;
    private ComparisonOperator operator;
    private Object operand;

    public ConditionDto() {
    }

    public ConditionDto(String attribute, ComparisonOperator operator, Object operand) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    public Object getOperand() {
        return operand;
    }

    public void setOperand(Object operand) {
        this.operand = operand;
    }
}
