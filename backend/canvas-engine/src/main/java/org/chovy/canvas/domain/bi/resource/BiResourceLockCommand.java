package org.chovy.canvas.domain.bi.resource;

/**
 * BiResourceLockCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param lockToken lockToken 字段。
 * @param ttlSeconds ttlSeconds 字段。
 */
public record BiResourceLockCommand(
        String resourceType,
        String resourceKey,
        String lockToken,
        Integer ttlSeconds) {
}
