package com.gabrieldeborba.voting.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every exception into an RFC 7807 {@link ProblemDetail} response.
 *
 * <ul>
 *   <li>{@link DomainException} → the status carried by the exception (404, 409, 422...)
 *   <li>Bean validation failures (body fields or query/path parameters) → 400 with an {@code errors}
 *       list of field/message pairs
 *   <li>Query/path parameter of the wrong type (e.g. a non-UUID id) → 400 with the same {@code errors} list
 *   <li>Unreadable JSON (malformed body, unknown enum value) → 400
 *   <li>Unknown route → 404 with the method and path in the detail
 *   <li>Other framework exceptions (wrong method, wrong media type) → handled by
 *       {@link ResponseEntityExceptionHandler}
 *   <li>Anything else → 500 with a generic message; details go to the log only
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TIMESTAMP_PROPERTY = "timestamp";

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn(
                "Business rule violated: status={} path={} message=\"{}\"",
                ex.getStatus().value(),
                request.getRequestURI(),
                ex.getMessage());
        return problem(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: path={}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : "unknown";
        String expectedType = ex.getRequiredType() == null ? "value" : ex.getRequiredType().getSimpleName();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", List.of(new ValidationError(field, "must be a valid " + expectedType)));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Malformed request body");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.NOT_FOUND, "No endpoint for " + ex.getHttpMethod() + " /" + ex.getResourcePath());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.NOT_FOUND, request);
    }

    /** Adds the timestamp to problems built by the parent class (unknown route, wrong method...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response.getBody() instanceof ProblemDetail problem && !hasTimestamp(problem)) {
            problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        }
        return response;
    }

    private static boolean hasTimestamp(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(TIMESTAMP_PROPERTY);
    }

    private static ProblemDetail problem(HttpStatus status, @Nullable String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
