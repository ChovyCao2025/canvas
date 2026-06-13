package org.chovy.canvas.canvas.application.dsl;

import java.util.List;

public record CanvasDslValidationResult(boolean valid, List<Violation> violations) {

    public CanvasDslValidationResult {
        violations = List.copyOf(violations == null ? List.of() : violations);
    }

    public static CanvasDslValidationResult passed() {
        return new CanvasDslValidationResult(true, List.of());
    }

    public static CanvasDslValidationResult failed(List<Violation> violations) {
        return new CanvasDslValidationResult(false, violations);
    }

    public record Violation(String code, String message) {

        public Violation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }
}
