package com.iquenobot.shared.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }

    public ForbiddenException() {
        super("You do not have permission to perform this action", "FORBIDDEN");
    }
}