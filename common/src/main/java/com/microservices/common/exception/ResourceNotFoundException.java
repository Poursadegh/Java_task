package com.microservices.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            "RESOURCE_NOT_FOUND",
            String.format("%s not found with identifier: %s", resourceName, identifier),
            HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}

