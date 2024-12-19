package com.gt.global.common.exception;

public class EntityNotSaveException extends RuntimeException {
    
    public EntityNotSaveException(String message) {
        super(message);
    }

    public EntityNotSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
