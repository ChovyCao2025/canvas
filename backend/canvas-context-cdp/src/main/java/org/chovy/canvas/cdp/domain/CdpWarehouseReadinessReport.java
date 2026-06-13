package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CdpWarehouseReadinessReport(
        Long tenantId,
        String status,
        LocalDateTime generatedAt,
        List<CdpWarehouseReadinessSection> sections) {
}
