package com.gabrieldeborba.voting.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for business rule violations. Each subclass carries the HTTP status the API should
 * answer with, so the {@link GlobalExceptionHandler} can translate it without a per-exception mapping.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;

    protected DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Use when translating a lower-level failure (e.g. a constraint violation) so the log keeps the cause. */
    protected DomainException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
