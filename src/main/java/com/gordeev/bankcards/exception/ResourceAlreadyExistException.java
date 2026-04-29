package com.gordeev.bankcards.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistException extends BusinessException {
    public static final String CODE = "RESOURCE_ALREADY_EXISTS";

    public ResourceAlreadyExistException(String message) {
        super(message, CODE, HttpStatus.CONFLICT);
    }
}
