package org.chovy.canvas.canvas.api;

import java.util.List;

public interface ApiDefinitionFacade {

    PageView<ApiDefinitionView> list(ApiDefinitionListQuery query);

    ApiDefinitionView create(ApiDefinitionCommand command);

    ApiDefinitionView update(Long id, ApiDefinitionCommand command);

    void delete(Long id);

    record ApiDefinitionListQuery(int page, int size, Integer enabled) {
    }

    record ApiDefinitionCommand(
            String apiKey,
            String url,
            Integer enabled,
            Integer includeContextPayload,
            Integer receiptEnabled,
            Integer receiptExpireMinutes,
            String receiptStatuses,
            Integer rateLimitPerSec,
            boolean rateLimitPerSecPresent) {
    }

    record ApiDefinitionView(
            Long id,
            String apiKey,
            String url,
            Integer enabled,
            Integer includeContextPayload,
            Integer receiptEnabled,
            Integer receiptExpireMinutes,
            String receiptStatuses,
            Integer rateLimitPerSec) {
    }

    record PageView<T>(long total, List<T> records) {
        public PageView {
            records = records == null ? List.of() : List.copyOf(records);
        }
    }
}
