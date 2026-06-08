package org.chovy.canvas.domain.bi.resource;

/**
 * BiResourceTransferCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param ownerUser ownerUser 字段。
 */
public record BiResourceTransferCommand(
        String resourceType,
        String resourceKey,
        String ownerUser) {
}
