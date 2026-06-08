package org.chovy.canvas.domain.bi.resource;

/**
 * BiResourceMoveCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param folderKey folderKey 字段。
 * @param sortOrder sortOrder 字段。
 */
public record BiResourceMoveCommand(
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder) {
}
