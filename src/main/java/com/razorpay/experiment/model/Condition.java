package com.razorpay.experiment.model;

import com.razorpay.experiment.enums.ComparisonOperator;

import java.util.Objects;

public final class Condition {

    private final String attribute;
    private final ComparisonOperator operator;
    private final Object operand;

    public Condition(String attribute, ComparisonOperator operator, Object operand) {
        this.attribute = attribute;
        this.operator = operator;
        this.operand = operand;
    }

    public String getAttribute() {
        return attribute;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public Object getOperand() {
        return operand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Condition condition = (Condition) o;
        return Objects.equals(attribute, condition.attribute)
                && operator == condition.operator
                && Objects.equals(operand, condition.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, operator, operand);
    }
}
