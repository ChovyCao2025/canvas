package org.chovy.canvas.domain.bi.resource;

/**
 * BiResourceCommentCommand 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param widgetKey widgetKey 字段。
 * @param commentText commentText 字段。
 */
public record BiResourceCommentCommand(
        String resourceType,
        String resourceKey,
        String widgetKey,
        String commentText) {
}
