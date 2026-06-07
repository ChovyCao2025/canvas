package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResource;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetResourceService;
import org.chovy.canvas.domain.bi.spreadsheet.BiSpreadsheetVersionView;
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

class BiSpreadsheetControllerTest {

    @Test
    void saveDraftUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiSpreadsheetResourceService service = mock(BiSpreadsheetResourceService.class);
        BiSpreadsheetResource resource = spreadsheet("DRAFT", 1);
        when(service.saveDraft(7L, "alice", resource)).thenReturn(resource);
        BiSpreadsheetController controller = new BiSpreadsheetController(resolver, service);

        StepVerifier.create(controller.saveDraft("campaign-weekly", resource))
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
        BiSpreadsheetResourceService service = mock(BiSpreadsheetResourceService.class);
        BiSpreadsheetResource resource = spreadsheet("PUBLISHED", 2);
        when(service.publish(7L, "alice", "campaign-weekly")).thenReturn(resource);
        BiSpreadsheetController controller = new BiSpreadsheetController(resolver, service);

        StepVerifier.create(controller.publish("campaign-weekly"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PUBLISHED"))
                .verifyComplete();

        verify(service).publish(7L, "alice", "campaign-weekly");
    }

    @Test
    void listVersionsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        BiSpreadsheetResourceService service = mock(BiSpreadsheetResourceService.class);
        BiSpreadsheetVersionView version = new BiSpreadsheetVersionView(
                4L,
                "campaign-weekly",
                2,
                "PUBLISHED",
                spreadsheet("PUBLISHED", 2),
                "alice",
                LocalDateTime.parse("2026-06-05T12:00:00"));
        when(service.listVersions(7L, "campaign-weekly", 20)).thenReturn(List.of(version));
        BiSpreadsheetController controller = new BiSpreadsheetController(resolver, service);

        StepVerifier.create(controller.listVersions("campaign-weekly", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.version()).isEqualTo(2)))
                .verifyComplete();

        verify(service).listVersions(7L, "campaign-weekly", 20);
    }

    private BiSpreadsheetResource spreadsheet(String status, int version) {
        return new BiSpreadsheetResource(
                88L,
                "campaign-weekly",
                "Campaign Weekly",
                "Formatted weekly campaign report",
                List.of(Map.of("sheetKey", "summary", "cells", Map.of("B9", "=SUM(B2:B8)"))),
                Map.of("datasetKey", "canvas_daily_stats", "columns", List.of("stat_date", "total_executions")),
                Map.of("B9", "currency"),
                status,
                version,
                "PERSISTED");
    }
}
