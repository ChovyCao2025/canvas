package org.chovy.canvas.cdp.api;

import java.util.Map;

public interface CdpIdentityTypeFacade {

    Map<String, Object> list(Integer enabled, Integer allowImport);

    Map<String, Object> create(Map<String, Object> payload);

    Map<String, Object> update(Long id, Map<String, Object> payload);

    Map<String, Object> delete(Long id);
}
