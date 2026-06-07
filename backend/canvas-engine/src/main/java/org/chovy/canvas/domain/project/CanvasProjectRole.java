package org.chovy.canvas.domain.project;

public enum CanvasProjectRole {
    PROJECT_ADMIN,
    EDITOR,
    EXECUTOR,
    VIEWER;

    public static CanvasProjectRole parse(String raw) {
        try {
            return CanvasProjectRole.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported project role: " + raw);
        }
    }
}
