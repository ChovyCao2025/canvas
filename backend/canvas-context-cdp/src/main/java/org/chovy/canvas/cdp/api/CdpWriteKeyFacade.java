package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

public interface CdpWriteKeyFacade {

    List<KeyRow> list(Long tenantId);

    CreateResult create(Long tenantId, CreateCommand command, String actor);

    void disable(Long tenantId, Long id);

    record CreateCommand(String name, String platform, Integer rateLimitQps, Long dailyQuota, String description) {
    }

    record CreateResult(Long id, String name, String rawKey, String keyPrefix, String platform, Integer rateLimitQps,
                        Long dailyQuota) {
    }

    record KeyRow(Long id, String name, String keyPrefix, String platform, String status, Integer rateLimitQps,
                  Long dailyQuota, String description, String createdBy, LocalDateTime createdAt,
                  LocalDateTime updatedAt) {
    }
}
