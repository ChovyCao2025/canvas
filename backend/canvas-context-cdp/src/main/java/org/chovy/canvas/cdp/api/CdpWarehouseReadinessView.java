package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

public record CdpWarehouseReadinessView(
        Long tenantId,
        String status,
        LocalDateTime generatedAt,
        List<CdpWarehouseReadinessSectionView> sections) {
}
