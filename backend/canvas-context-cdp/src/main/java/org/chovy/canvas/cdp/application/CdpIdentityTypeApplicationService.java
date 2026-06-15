package org.chovy.canvas.cdp.application;

import java.util.Map;

import org.chovy.canvas.cdp.api.CdpIdentityTypeFacade;
import org.chovy.canvas.cdp.domain.CdpIdentityTypeCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpIdentityTypeApplicationService implements CdpIdentityTypeFacade {

    private final CdpIdentityTypeCatalog catalog;

    public CdpIdentityTypeApplicationService() {
        this(new CdpIdentityTypeCatalog());
    }

    CdpIdentityTypeApplicationService(CdpIdentityTypeCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> list(Integer enabled, Integer allowImport) {
        return catalog.list(enabled, allowImport);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> payload) {
        return catalog.create(safePayload(payload));
    }

    @Override
    public Map<String, Object> update(Long id, Map<String, Object> payload) {
        return catalog.update(id, safePayload(payload));
    }

    @Override
    public Map<String, Object> delete(Long id) {
        return catalog.delete(id);
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }
}
