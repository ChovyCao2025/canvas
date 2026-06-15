package org.chovy.canvas.cdp.application;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpEventAttributeDiscoveryFacade;
import org.springframework.stereotype.Service;

@Service
public class CdpEventAttributeDiscoveryApplicationService implements CdpEventAttributeDiscoveryFacade {

    @Override
    public List<DiscoveredAttributeView> listDiscovered(String status) {
        List<DiscoveredAttributeView> rows = List.of(
                new DiscoveredAttributeView(
                        101L,
                        "USER_SIGNUP",
                        "plan",
                        "STRING",
                        "ACTIVE",
                        "pro",
                        "2026-06-01T10:00:00",
                        "2026-06-14T10:00:00"),
                new DiscoveredAttributeView(
                        102L,
                        "ORDER_PAID",
                        "amount",
                        "DECIMAL",
                        "ACTIVE",
                        "12.30",
                        "2026-06-02T10:00:00",
                        "2026-06-14T11:00:00"),
                new DiscoveredAttributeView(
                        103L,
                        "USER_SIGNUP",
                        "legacySource",
                        "STRING",
                        "INACTIVE",
                        "import",
                        "2026-06-03T10:00:00",
                        "2026-06-14T12:00:00"));
        if (status == null || status.isBlank()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> row.status().equalsIgnoreCase(status))
                .toList();
    }
}
