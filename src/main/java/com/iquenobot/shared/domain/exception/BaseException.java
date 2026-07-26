package com.iquenobot.shared.domain.exception;

public abstract class BaseException extends RuntimeException {

    private final int status;
    private final String errorCode;

    protected BaseException(String message, int status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

}