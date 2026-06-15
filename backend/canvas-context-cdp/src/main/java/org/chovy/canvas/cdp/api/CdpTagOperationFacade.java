package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CdpTagOperationFacade {

    TagOperationView create(Long tenantId, BatchTagCommand command, String actor);

    List<TagOperationView> listRecent(Long tenantId, int limit);

    TagOperationView get(Long tenantId, Long id);

    TagOperationView retryFailed(Long tenantId, Long id, String actor);

    record BatchTagCommand(
            String userId,
            String tagCode,
            String tagValue,
            List<String> memberIds,
            Map<String, Object> metadata) {
    }

    record TagOperationView(
            Long id,
            Long tenantId,
            String userId,
            String tagCode,
            String tagValue,
            List<String> memberIds,
            Map<String, Object> metadata,
            String status,
            int affectedCount,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
