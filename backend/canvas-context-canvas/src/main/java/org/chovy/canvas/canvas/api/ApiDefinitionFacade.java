package org.chovy.canvas.canvas.api;

import java.util.List;

/**
 * 定义ApiDefinitionFacade对外提供的能力契约。
 */
public interface ApiDefinitionFacade {

    /**
     * 列出。
     */
    PageView<ApiDefinitionView> list(ApiDefinitionListQuery query);

    /**
     * 创建。
     */
    ApiDefinitionView create(ApiDefinitionCommand command);

    /**
     * 更新。
     */
    ApiDefinitionView update(Long id, ApiDefinitionCommand command);

    /**
     * 删除。
     */
    void delete(Long id);

    /**
     * 承载ApiDefinitionListQuery的数据快照。
     */
    record ApiDefinitionListQuery(int page, int size, Integer enabled) {
    }

    /**
     * 承载ApiDefinitionCommand的数据快照。
     */
    record ApiDefinitionCommand(
            /**
             * 记录apiKey。
             */
            String apiKey,
            /**
             * 记录url。
             */
            String url,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            /**
             * 记录includeContextPayload。
             */
            Integer includeContextPayload,
            /**
             * 记录receiptEnabled。
             */
            Integer receiptEnabled,
            /**
             * 记录receiptExpireMinutes。
             */
            Integer receiptExpireMinutes,
            /**
             * 记录receiptStatuses。
             */
            String receiptStatuses,
            /**
             * 记录rateLimitPerSec。
             */
            Integer rateLimitPerSec,
            /**
             * 记录rateLimitPerSecPresent。
             */
            boolean rateLimitPerSecPresent) {
    }

    /**
     * 承载ApiDefinitionView的数据快照。
     */
    record ApiDefinitionView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录apiKey。
             */
            String apiKey,
            /**
             * 记录url。
             */
            String url,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            /**
             * 记录includeContextPayload。
             */
            Integer includeContextPayload,
            /**
             * 记录receiptEnabled。
             */
            Integer receiptEnabled,
            /**
             * 记录receiptExpireMinutes。
             */
            Integer receiptExpireMinutes,
            /**
             * 记录receiptStatuses。
             */
            String receiptStatuses,
            /**
             * 记录rateLimitPerSec。
             */
            Integer rateLimitPerSec) {
    }

    /**
     * 承载PageView的数据快照。
     */
    record PageView<T>(long total, List<T> records) {
        public PageView {
            records = records == null ? List.of() : List.copyOf(records);
        }
    }
}
