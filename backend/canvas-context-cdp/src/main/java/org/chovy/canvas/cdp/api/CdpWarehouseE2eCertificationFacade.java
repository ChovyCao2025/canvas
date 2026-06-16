package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseE2eCertificationFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseE2eCertificationFacade {

    Map<String, Object> certify(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            /**
             * require Data Path Proof)。
             */
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof);

    Map<String, Object> gate(Long tenantId, String mode, List<String> contractKeys, boolean requirePhysical,
            /**
             * max Age Minutes)。
             */
            boolean requireRealtime, boolean requireDataPathProof, Long maxAgeMinutes);

    Map<String, Object> run(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            /**
             * requested By)。
             */
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof, String requestedBy);

    /**
     * limit)。
     */
    List<Map<String, Object>> recent(Long tenantId, Integer limit);

    /**
     * id)。
     */
    Map<String, Object> get(Long tenantId, Long id);
}
