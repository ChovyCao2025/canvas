package org.chovy.canvas.canvas.domain;

/**
 * 承载Canvas的数据快照。
 */
public record Canvas(
        /**
         * 记录标识。
         */
        Long id,
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录名称。
         */
        String name,
        /**
         * 记录描述。
         */
        String description,
        /**
         * 记录状态。
         */
        CanvasStatus status,
        /**
         * 记录published version标识。
         */
        Long publishedVersionId,
        /**
         * 记录previous version标识。
         */
        Long previousVersionId,
        /**
         * 记录canary version标识。
         */
        Long canaryVersionId,
        /**
         * 记录canaryPercent。
         */
        Integer canaryPercent,
        /**
         * 记录创建人。
         */
        String createdBy,
        /**
         * 记录runtimeOptions。
         */
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

    /**
     * 创建草稿状态的画布。
     */
    public static Canvas createDraft(Long id, Long tenantId, String name, String description, String createdBy) {
        return new Canvas(id, tenantId, name, description, CanvasStatus.DRAFT,
                null, null, null, null, createdBy, CanvasRuntimeOptions.empty());
    }

    /**
     * 返回替换id后的不可变副本。
     */
    public Canvas withId(Long newId) {
        return new Canvas(newId, tenantId, name, description, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    /**
     * 返回替换runtime options后的不可变副本。
     */
    public Canvas withRuntimeOptions(CanvasRuntimeOptions newRuntimeOptions) {
        return new Canvas(id, tenantId, name, description, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, newRuntimeOptions);
    }

    /**
     * 返回更新基础元数据后的画布副本。
     */
    public Canvas updateMetadata(String newName, String newDescription) {
        return new Canvas(id, tenantId, newName, newDescription, status,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    /**
     * 返回发布后的画布状态副本。
     */
    public Canvas publish(Long versionId) {
        return new Canvas(id, tenantId, name, description, CanvasStatus.PUBLISHED,
                versionId, publishedVersionId, null, null, createdBy, runtimeOptions);
    }

    /**
     * 返回下线后的画布状态副本。
     */
    public Canvas offline() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.OFFLINE,
                null, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    /**
     * 返回归档后的画布状态副本。
     */
    public Canvas archive() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.ARCHIVED,
                publishedVersionId, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }

    /**
     * 返回终止后的画布状态副本。
     */
    public Canvas kill() {
        return new Canvas(id, tenantId, name, description, CanvasStatus.KILLED,
                null, previousVersionId, canaryVersionId, canaryPercent, createdBy, runtimeOptions);
    }
}
