package org.chovy.canvas.canvas.domain;

public record Canvas(
        Long id,
        Long tenantId,
        String name,
        String description,
        CanvasStatus status,
        Long publishedVersionId,
        Long previousVersionId,
        Long canaryVersionId,
        Integer canaryPercent,
        String createdBy,
        CanvasRuntimeOptions runtimeOptions) {

    public Canvas {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        status = status == null ? CanvasStatus.DRAFT : status;
        runtimeOptions = runtimeOptions == null ? CanvasRuntimeOptions.empty() : runtimeOptions;
    }

    public Canvas(Long id,
                  Long tenantId,
                  String name,
                  String description,
                  CanvasStatus status,
                  Long publishedVersionId,
                  Long previousVersionId,
                  Long canaryVersionId,
                  Integer canaryPercent,
                  String createdBy) {
        this(id, tenantId, name, description, status, publishedVersionId, previousVersionId,
                canaryVersionId, canaryPercent, createdBy, CanvasRuntimeOptions.empty());
    }

    public static Canvas createDraft(Long id, Long tenantId, String name, String description, String createdBy) {
        return new Canvas(id, tenantId, name, description, CanvasStatus.DRAFT,
                null, null, null, null, createdBy, CanvasRuntimeOptions.empty());
    }

    public Canvas withId(Long newId) {
        return new Canvas(newId, tenantId, name, description, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    public Canvas withRuntimeOptions(CanvasRuntimeOptions newRuntimeOptions) {
        return new Canvas(id, tenantId, name, description, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, newRuntimeOptions);
    }

    public Canvas updateMetadata(String newName, String newDescription) {
        return new Canvas(id, tenantId, newName, newDescription, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    public Canvas publish(Long versionId) {
        return new Canvas(id, tenantId, name, description, CanvasStatus.PUBLISHED,
                versionId, publishedVersionId, null, null, createdBy, runtimeOptions);
    }

    public Canvas offline() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.OFFLINE,
                null, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    public Canvas archive() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.ARCHIVED,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    public Canvas kill() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.KILLED,
                null, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }
}
