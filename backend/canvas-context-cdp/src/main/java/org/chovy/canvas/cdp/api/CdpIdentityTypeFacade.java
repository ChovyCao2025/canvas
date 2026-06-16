package org.chovy.canvas.cdp.api;

import java.util.Map;

/**
 * 定义 CdpIdentityTypeFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpIdentityTypeFacade {

    /**
     * allow Import)。
     */
    Map<String, Object> list(Integer enabled, Integer allowImport);

    /**
     * payload)。
     */
    Map<String, Object> create(Map<String, Object> payload);

    /**
     * payload)。
     */
    Map<String, Object> update(Long id, Map<String, Object> payload);

    /**
     * id)。
     */
    Map<String, Object> delete(Long id);
}
