package com.iquenobot.shared.exception;

public class InvalidStateException extends BusinessException {

    public InvalidStateException(String message) {
        super(message, "INVALID_STATE");
    }
}