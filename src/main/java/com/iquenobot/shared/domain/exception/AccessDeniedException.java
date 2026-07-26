package com.iquenobot.shared.domain.exception;

public class AccessDeniedException extends BaseException {

    public AccessDeniedException(String message) {
        super(message, 403, "ACCESS_DENIED");
    }

}