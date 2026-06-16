package org.chovy.canvas.canvas.application.dsl;

import java.util.List;

/**
 * 承载CanvasDslValidationResult的数据快照。
 */
public record CanvasDslValidationResult(boolean valid, List<Violation> violations) {

    public CanvasDslValidationResult {
        violations = List.copyOf(violations == null ? List.of() : violations);
    }

    /**
     * 处理passed。
     */
    public static CanvasDslValidationResult passed() {
        return new CanvasDslValidationResult(true, List.of());
    }

    /**
     * 处理failed。
     */
    public static CanvasDslValidationResult failed(List<Violation> violations) {
        return new CanvasDslValidationResult(false, violations);
    }

    /**
     * 承载Violation的数据快照。
     */
    public record Violation(String code, String message) {

        public Violation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }
}
