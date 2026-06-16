package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiQueryGovernanceAuditEntryView 视图。
 */
public record BiQueryGovernanceAuditEntryView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * actionKey 对应的业务键。
         */
        String actionKey,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 操作者。
         */
        String actor,
        /**
         * detail 字段值。
         */
        Map<String, Object> detail,
        LocalDateTime createdAt) {

    public BiQueryGovernanceAuditEntryView {
        detail = detail == null ? Map.of() : Map.copyOf(detail);
    }
}
