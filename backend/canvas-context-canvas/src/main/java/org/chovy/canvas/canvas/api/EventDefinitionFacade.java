package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.List;

public interface EventDefinitionFacade {

    PageView<EventDefinitionView> list(EventDefinitionListQuery query);

    EventDefinitionView create(EventDefinitionCommand command);

    EventDefinitionView update(Long id, EventDefinitionCommand command);

    void delete(Long id);

    record EventDefinitionListQuery(int page, int size, Integer enabled) {
    }

    record EventDefinitionCommand(
            String name,
            String eventCode,
            String attributes,
            String description,
            Integer autoDiscover,
            String discoveryMode,
            Integer enabled,
            String createdBy) {
    }

    record EventDefinitionView(
            Long id,
            String name,
            String eventCode,
            String attributes,
            String description,
            Integer autoDiscover,
            String discoveryMode,
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
