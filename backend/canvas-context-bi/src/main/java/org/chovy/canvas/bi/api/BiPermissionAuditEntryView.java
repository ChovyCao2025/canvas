package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiPermissionAuditEntryView 视图。
 */
public record BiPermissionAuditEntryView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * actorId 对应的标识。
         */
        String actorId,
        /**
         * actionKey 对应的业务键。
         */
        String actionKey,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * detailJson 的 JSON 序列化内容。
         */
        String detailJson,
        LocalDateTime createdAt) {
}
