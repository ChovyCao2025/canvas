package org.chovy.canvas.canvas.domain;

public record CanvasVersion(
        Long id,
        Long canvasId,
        Long tenantId,
        Integer version,
        String graphJson,
        VersionStatus status,
        String createdBy) {

    public CanvasVersion {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version is required");
        }
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("graphJson is required");
        }
        status = status == null ? VersionStatus.DRAFT : status;
    }

    public static CanvasVersion draft(Long id, Long canvasId, Long tenantId, Integer version,
                                      String graphJson, String createdBy) {
        return new CanvasVersion(id, canvasId, tenantId, version, graphJson, VersionStatus.DRAFT, createdBy);
    }

    public static CanvasVersion published(Long id, Long canvasId, Long tenantId, Integer version,
                                          String graphJson, String createdBy) {
        return new CanvasVersion(id, canvasId, tenantId, version, graphJson, VersionStatus.PUBLISHED, createdBy);
    }

    public CanvasVersion withId(Long newId) {
        return new CanvasVersion(newId, canvasId, tenantId, version, graphJson, status, createdBy);
    }

    public CanvasVersion withGraphJson(String newGraphJson) {
        return new CanvasVersion(id, canvasId, tenantId, version, newGraphJson, status, createdBy);
    }
}
