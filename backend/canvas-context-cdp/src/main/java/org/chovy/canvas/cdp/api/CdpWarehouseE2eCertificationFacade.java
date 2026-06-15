package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseE2eCertificationFacade {

    Map<String, Object> certify(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof);

    Map<String, Object> gate(Long tenantId, String mode, List<String> contractKeys, boolean requirePhysical,
            boolean requireRealtime, boolean requireDataPathProof, Long maxAgeMinutes);

    Map<String, Object> run(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof, String requestedBy);

    List<Map<String, Object>> recent(Long tenantId, Integer limit);

    Map<String, Object> get(Long tenantId, Long id);
}
