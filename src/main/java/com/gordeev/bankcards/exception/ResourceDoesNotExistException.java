package com.gordeev.bankcards.exception;

import org.springframework.http.HttpStatus;

public class ResourceDoesNotExistException extends BusinessException {
    public static final String CODE = "RESOURCE_DOES_NOT_EXIST";

    public ResourceDoesNotExistException(String message) {
        super(message, CODE, HttpStatus.CONFLICT);
    }
}
