package com.iquenobot.shared.domain.exception;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s no encontrado con id: %s".formatted(resource, id), 404, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, 404, "RESOURCE_NOT_FOUND");
    }

}