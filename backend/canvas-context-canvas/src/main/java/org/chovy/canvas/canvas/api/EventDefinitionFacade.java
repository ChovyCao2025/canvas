package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义EventDefinitionFacade对外提供的能力契约。
 */
public interface EventDefinitionFacade {

    /**
     * 列出。
     */
    PageView<EventDefinitionView> list(EventDefinitionListQuery query);

    /**
     * 创建。
     */
    EventDefinitionView create(EventDefinitionCommand command);

    /**
     * 更新。
     */
    EventDefinitionView update(Long id, EventDefinitionCommand command);

    /**
     * 删除。
     */
    void delete(Long id);

    /**
     * 承载EventDefinitionListQuery的数据快照。
     */
    record EventDefinitionListQuery(int page, int size, Integer enabled) {
    }

    /**
     * 承载EventDefinitionCommand的数据快照。
     */
    record EventDefinitionCommand(
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录eventCode。
             */
            String eventCode,
            /**
             * 记录attributes。
             */
            String attributes,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录autoDiscover。
             */
            Integer autoDiscover,
            /**
             * 记录discoveryMode。
             */
            String discoveryMode,
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
     * 承载EventDefinitionView的数据快照。
     */
    record EventDefinitionView(
            /**
             * 记录标识。
             */
            Long id,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录eventCode。
             */
            String eventCode,
            /**
             * 记录attributes。
             */
            String attributes,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录autoDiscover。
             */
            Integer autoDiscover,
            /**
             * 记录discoveryMode。
             */
            String discoveryMode,
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
