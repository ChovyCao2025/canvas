package org.chovy.canvas.bi.api;

import java.util.List;
/**
 * BiQuickEngineQueueSnapshotView 视图。
 */
public record BiQuickEngineQueueSnapshotView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * poolKey 对应的业务键。
         */
        String poolKey,
        /**
         * queued 字段值。
         */
        Long queued,
        /**
         * claimed 字段值。
         */
        Long claimed,
        /**
         * completed 字段值。
         */
        Long completed,
        /**
         * blocked 字段值。
         */
        Long blocked,
        /**
         * total 对应的统计数量。
         */
        Long total,
        List<BiQuickEngineQueueItemView> jobs) {
}
