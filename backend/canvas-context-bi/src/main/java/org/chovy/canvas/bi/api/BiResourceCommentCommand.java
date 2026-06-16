package org.chovy.canvas.bi.api;
/**
 * BiResourceCommentCommand 命令。
 */
public record BiResourceCommentCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * widgetKey 对应的业务键。
         */
        String widgetKey,
        String commentText) {
}
