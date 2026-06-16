package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义MqDefinitionFacade对外提供的能力契约。
 */
public interface MqDefinitionFacade {

    /**
     * 列出。
     */
    PageView<MqDefinitionView> list(MqDefinitionListQuery query);

    /**
     * 创建。
     */
    MqDefinitionView create(MqDefinitionCommand command);

    /**
     * 更新。
     */
    MqDefinitionView update(Long id, MqDefinitionCommand command);

    /**
     * 删除。
     */
    void delete(Long id);

    /**
     * 承载MqDefinitionListQuery的数据快照。
     */
    record MqDefinitionListQuery(int page, int size, Integer enabled) {
    }

    /**
     * 承载MqDefinitionCommand的数据快照。
     */
    record MqDefinitionCommand(
            /**
             * 记录messageCode。
             */
            String messageCode,
            /**
             * 记录topic。
             */
            String topic,
            /**
             * 记录tags。
             */
            String tags,
            /**
             * 记录consumerGroup。
             */
            String consumerGroup,
            /**
             * 记录payloadSchema。
             */
            String payloadSchema,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            /**
             * 记录创建人。
             */
            String createdBy) {
    }

    /**
     * 承载MqDefinitionView的数据快照。
     */
    record MqDefinitionView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录messageCode。
             */
            String messageCode,
            /**
             * 记录topic。
             */
            String topic,
            /**
             * 记录tags。
             */
            String tags,
            /**
             * 记录consumerGroup。
             */
            String consumerGroup,
            /**
             * 记录payloadSchema。
             */
            String payloadSchema,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录启用状态。
             */
            Integer enabled,
            /**
             * 记录创建人。
             */
            String createdBy,
            /**
             * 记录创建时间。
             */
            LocalDateTime createdAt,
            /**
             * 记录更新时间。
             */
            LocalDateTime updatedAt) {
    }

    /**
     * 承载PageView的数据快照。
     */
    record PageView<T>(long total, List<T> list) {
        public PageView {
            list = list == null ? List.of() : List.copyOf(list);
        }
    }
}
