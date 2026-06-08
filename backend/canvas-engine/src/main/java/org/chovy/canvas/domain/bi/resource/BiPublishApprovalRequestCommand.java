package org.chovy.canvas.domain.bi.resource;

/**
 * BiPublishApprovalRequestCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param reason reason 字段。
 */
public record BiPublishApprovalRequestCommand(
        String resourceType,
        String resourceKey,
        String reason) {
}
