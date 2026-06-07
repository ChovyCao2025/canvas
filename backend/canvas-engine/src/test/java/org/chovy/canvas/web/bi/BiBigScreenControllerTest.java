package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResource;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenResourceService;
import org.chovy.canvas.domain.bi.bigscreen.BiBigScreenVersionView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiBigScreenControllerTest {

    @Test
    void saveDraftUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiBigScreenResourceService service = mock(BiBigScreenResourceService.class);
        BiBigScreenResource resource = screen("DRAFT", 1);
        when(service.saveDraft(7L, "alice", resource)).thenReturn(resource);
        BiBigScreenController controller = new BiBigScreenController(resolver, service);

        StepVerifier.create(controller.saveDraft("ops-war-room", resource))
                .assertNext(response -> {
                    assertThat(response.getData().status()).isEqualTo("DRAFT");
                    assertThat(response.getData().version()).isEqualTo(1);
                })
                .verifyComplete();

        verify(service).saveDraft(7L, "alice", resource);
    }

    @Test
    void publishUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiBigScreenResourceService service = mock(BiBigScreenResourceService.class);
        BiBigScreenResource resource = screen("PUBLISHED", 2);
        when(service.publish(7L, "alice", "ops-war-room")).thenReturn(resource);
        BiBigScreenController controller = new BiBigScreenController(resolver, service);

        StepVerifier.create(controller.publish("ops-war-room"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "ops-war-room");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiBigScreenResourceService service = mock(BiBigScreenResourceService.class);
        BiBigScreenVersionView version = new BiBigScreenVersionView(
                3L,
                "ops-war-room",
                2,
                "PUBLISHED",
                screen("PUBLISHED", 2),
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"));
        when(service.listVersions(7L, "ops-war-room", 20)).thenReturn(List.of(version));
        BiBigScreenController controller = new BiBigScreenController(resolver, service);

        StepVerifier.create(controller.listVersions("ops-war-room", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.version()).isEqualTo(2)))
                .verifyComplete();

        verify(service).listVersions(7L, "ops-war-room", 20);
    }

    private BiBigScreenResource screen(String status, int version) {
        return new BiBigScreenResource(
                99L,
                "ops-war-room",
                "Ops War Room",
                "Campaign command center",
                Map.of("width", 1920, "height", 1080),
                Map.of("color", "#101820"),
                List.of(Map.of("widgetKey", "kpi-total", "x", 0, "y", 0, "w", 6, "h", 4)),
                Map.of("intervalSeconds", 30),
                Map.of("phone", List.of(Map.of("widgetKey", "kpi-total", "order", 1))),
                status,
                version,
                "PERSISTED");
    }
}
