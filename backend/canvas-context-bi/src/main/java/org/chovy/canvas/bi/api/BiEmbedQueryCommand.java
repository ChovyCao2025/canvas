package org.chovy.canvas.bi.api;
/**
 * BiEmbedQueryCommand 命令。
 */
public record BiEmbedQueryCommand(
        /**
         * ticket 字段值。
         */
        String ticket,
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
        BiQueryCommand query) {
}
