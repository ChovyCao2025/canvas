package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseEnterpriseOlapEvidenceFacade {

    Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor);

    Map<String, Object> latest(Long tenantId);

    List<Map<String, Object>> proof(Long tenantId);

    Map<String, Object> collect(Long tenantId, String triggerType, String actor);

    List<Map<String, Object>> collections(Long tenantId, Integer limit);

    record EvidenceCommand(
            String evidenceKey,
            String status,
            String reason,
            String measuredAt,
            String expiresAt,
            String evidenceJson) {
    }
}
