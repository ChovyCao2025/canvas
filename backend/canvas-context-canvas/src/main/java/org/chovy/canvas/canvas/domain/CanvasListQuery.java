package org.chovy.canvas.canvas.domain;

/**
 * 画布列表查询条件。
 */
public record CanvasListQuery(
        int page,
        int size,
        Integer status,
        String name,
        Long tenantId,
        String projectKey,
        Long projectId,
        String folderKey) {

    public CanvasListQuery {
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 20 : Math.min(size, 200);
        name = blankToNull(name);
        projectKey = blankToNull(projectKey);
        folderKey = blankToNull(folderKey);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
