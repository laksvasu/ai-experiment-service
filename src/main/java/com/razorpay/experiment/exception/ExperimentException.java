package com.razorpay.experiment.exception;

public class ExperimentException extends RuntimeException {

    public ExperimentException(String message) {
        super(message);
    }

    public ExperimentException(String message, Throwable cause) {
        super(message, cause);
    }
}
