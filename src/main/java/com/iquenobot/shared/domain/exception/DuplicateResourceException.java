package com.iquenobot.shared.domain.exception;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, 409, "DUPLICATE_RESOURCE");
    }

}