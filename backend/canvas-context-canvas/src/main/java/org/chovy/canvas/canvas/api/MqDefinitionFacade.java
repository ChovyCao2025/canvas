package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;

public interface MqDefinitionFacade {

    PageView<MqDefinitionView> list(MqDefinitionListQuery query);

    MqDefinitionView create(MqDefinitionCommand command);

    MqDefinitionView update(Long id, MqDefinitionCommand command);

    void delete(Long id);

    record MqDefinitionListQuery(int page, int size, Integer enabled) {
    }

    record MqDefinitionCommand(
            String messageCode,
            String topic,
            String tags,
            String consumerGroup,
            String payloadSchema,
            String description,
            Integer enabled,
            String createdBy) {
    }

    record MqDefinitionView(
            Long id,
            String messageCode,
            String topic,
            String tags,
            String consumerGroup,
            String payloadSchema,
            String description,
            Integer enabled,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    record PageView<T>(long total, List<T> list) {
        public PageView {
            list = list == null ? List.of() : List.copyOf(list);
        }
    }
}
