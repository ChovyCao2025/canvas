package org.chovy.canvas.canvas.domain;

/**
 * 承载CanvasVersion的数据快照。
 */
public record CanvasVersion(
        /**
         * 记录标识。
         */
        Long id,
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录version。
         */
        Integer version,
        /**
         * 记录graphJSON 内容。
         */
        String graphJson,
        /**
         * 记录状态。
         */
        VersionStatus status,
        /**
         * 记录创建人。
         */
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

    /**
     * 返回替换id后的不可变副本。
     */
    public CanvasVersion withId(Long newId) {
        return new CanvasVersion(newId, canvasId, tenantId, version, graphJson, status, createdBy);
    }

    /**
     * 返回替换graph json后的不可变副本。
     */
    public CanvasVersion withGraphJson(String newGraphJson) {
        return new CanvasVersion(id, canvasId, tenantId, version, newGraphJson, status, createdBy);
    }
}
