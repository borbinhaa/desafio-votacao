package com.gabrieldeborba.voting.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
