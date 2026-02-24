package com.codeops.exception;

import java.util.UUID;

public class NotFoundException extends CodeOpsException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entityName, UUID id) {
        super(entityName + " not found with id: " + id);
    }

    public NotFoundException(String entityName, String field, String value) {
        super(entityName + " not found with " + field + ": " + value);
    }
}
