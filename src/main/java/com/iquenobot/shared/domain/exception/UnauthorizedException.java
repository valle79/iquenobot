package com.iquenobot.shared.domain.exception;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, 401, "UNAUTHORIZED");
    }

}