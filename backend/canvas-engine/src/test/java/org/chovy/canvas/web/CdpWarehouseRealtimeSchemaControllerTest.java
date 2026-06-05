package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeSchemaService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeSchemaControllerTest {

    @Test
    void registerUsesCurrentTenantAndExplicitOperator() {
        CdpWarehouseRealtimeSchemaService service = mock(CdpWarehouseRealtimeSchemaService.class);
        CdpWarehouseRealtimeSchemaService.SchemaVersionView view = view("1");
        when(service.register(eq(9L), any(), eq("data-owner"))).thenReturn(view);
        CdpWarehouseRealtimeSchemaController.SchemaVersionReq req =
                new CdpWarehouseRealtimeSchemaController.SchemaVersionReq();
        req.setPipelineKey("pipe");
        req.setSchemaRole("SOURCE");
        req.setSchemaVersion("1");
        req.setSchemaJson(schemaJson());
        req.setRegisteredBy("data-owner");
        CdpWarehouseRealtimeSchemaController controller =
                new CdpWarehouseRealtimeSchemaController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseRealtimeSchemaService.SchemaVersionView> response =
                controller.register(req).block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).register(eq(9L), org.mockito.ArgumentMatchers.argThat(command ->
                "pipe".equals(command.pipelineKey())
                        && "SOURCE".equals(command.schemaRole())
                        && "1".equals(command.schemaVersion())), eq("data-owner"));
    }

    @Test
    void listUsesCurrentTenantAndFilters() {
        CdpWarehouseRealtimeSchemaService service = mock(CdpWarehouseRealtimeSchemaService.class);
        List<CdpWarehouseRealtimeSchemaService.SchemaVersionView> rows = List.of(view("1"));
        when(service.list(9L, "pipe", "SOURCE", 20)).thenReturn(rows);
        CdpWarehouseRealtimeSchemaController controller =
                new CdpWarehouseRealtimeSchemaController(service, tenantResolver(9L, "alice"));

        R<List<CdpWarehouseRealtimeSchemaService.SchemaVersionView>> response =
                controller.list("pipe", "SOURCE", 20).block();

        assertThat(response.getData()).isSameAs(rows);
        verify(service).list(9L, "pipe", "SOURCE", 20);
    }

    @Test
    void latestUsesCurrentTenant() {
        CdpWarehouseRealtimeSchemaService service = mock(CdpWarehouseRealtimeSchemaService.class);
        CdpWarehouseRealtimeSchemaService.SchemaVersionView view = view("2");
        when(service.latest(9L, "pipe", "SINK")).thenReturn(view);
        CdpWarehouseRealtimeSchemaController controller =
                new CdpWarehouseRealtimeSchemaController(service, tenantResolver(9L, "alice"));

        R<CdpWarehouseRealtimeSchemaService.SchemaVersionView> response =
                controller.latest("pipe", "SINK").block();

        assertThat(response.getData()).isSameAs(view);
        verify(service).latest(9L, "pipe", "SINK");
    }

    private CdpWarehouseRealtimeSchemaService.SchemaVersionView view(String version) {
        return new CdpWarehouseRealtimeSchemaService.SchemaVersionView(
                1L,
                9L,
                "pipe",
                "SOURCE",
                version,
                "hash",
                schemaJson(),
                "BACKWARD",
                "COMPATIBLE",
                null,
                true,
                "alice",
                null,
                null);
    }

    private String schemaJson() {
        return """
                {"fields":[{"name":"event_id","type":"STRING","nullable":false}]}
                """;
    }

    private TenantContextResolver tenantResolver(Long tenantId, String username) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, "TENANT_ADMIN", username)));
        return resolver;
    }
}
