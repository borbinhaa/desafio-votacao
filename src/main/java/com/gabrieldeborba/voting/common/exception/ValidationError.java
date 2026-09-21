package com.gabrieldeborba.voting.common.exception;

/** One invalid field in a request body, listed under the {@code errors} property of a problem response. */
public record ValidationError(String field, String message) {}
