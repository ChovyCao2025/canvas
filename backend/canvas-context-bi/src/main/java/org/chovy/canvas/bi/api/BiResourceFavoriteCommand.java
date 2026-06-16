package org.chovy.canvas.bi.api;
/**
 * BiResourceFavoriteCommand 命令。
 */
public record BiResourceFavoriteCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        String title) {
}
