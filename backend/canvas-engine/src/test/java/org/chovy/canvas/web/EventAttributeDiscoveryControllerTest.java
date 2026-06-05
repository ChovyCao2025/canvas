package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO;
import org.chovy.canvas.domain.cdp.EventAttributeDiscoveryService;
import org.chovy.canvas.dto.cdp.CdpDiscoveredAttributeDTO;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventAttributeDiscoveryControllerTest {

    @Test
    void listDiscoveredAttributesUsesCurrentTenantAndMapsRows() {
        EventAttributeDiscoveryService discoveryService = mock(EventAttributeDiscoveryService.class);
        EventAttributeDiscoveryController controller =
                new EventAttributeDiscoveryController(tenantResolver(42L, "alice"), discoveryService);
        when(discoveryService.list(42L, EventAttrDefinitionDO.PENDING_REVIEW)).thenReturn(List.of(row()));

        List<CdpDiscoveredAttributeDTO> rows = controller
                .listDiscovered(EventAttrDefinitionDO.PENDING_REVIEW)
                .block()
                .getData();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).eventCode()).isEqualTo("OrderComplete");
        assertThat(rows.get(0).attrName()).isEqualTo("currency");
        assertThat(rows.get(0).status()).isEqualTo(EventAttrDefinitionDO.PENDING_REVIEW);
        verify(discoveryService).list(42L, EventAttrDefinitionDO.PENDING_REVIEW);
    }

    private EventAttrDefinitionDO row() {
        EventAttrDefinitionDO row = new EventAttrDefinitionDO();
        row.setId(7L);
        row.setTenantId(42L);
        row.setEventCode("OrderComplete");
        row.setAttrName("currency");
        row.setAttrType("STRING");
        row.setStatus(EventAttrDefinitionDO.PENDING_REVIEW);
        row.setSampleValue("CNY");
        row.setFirstSeenAt(LocalDateTime.parse("2026-06-05T10:00:00"));
        row.setLastSeenAt(LocalDateTime.parse("2026-06-05T10:05:00"));
        return row;
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
