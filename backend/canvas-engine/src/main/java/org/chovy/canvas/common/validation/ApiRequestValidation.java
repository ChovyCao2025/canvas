package org.chovy.canvas.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/** Boundary validation helper for raw-body endpoints that verify signatures before JSON binding. */
public final class ApiRequestValidation {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private ApiRequestValidation() {}

    public static <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message(violations));
        }
        return request;
    }

    private static <T> String message(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
    }
}
