package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceCollectionService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseEnterpriseOlapEvidenceControllerTest {

    @Test
    void requestMappingUsesWarehouseNamespace() {
        RequestMapping mapping = CdpWarehouseEnterpriseOlapEvidenceController.class
                .getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).containsExactly("/warehouse/enterprise-olap/evidence");
    }

    @Test
    void recordOperatorEvidenceUsesTenantAndActorFromContext() {
        CdpWarehouseEnterpriseOlapEvidenceService service =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        TenantContextResolver resolver = resolver();
        CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand command =
                new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand(
                        "backup_restore",
                        "PASS",
                        "restore drill passed",
                        LocalDateTime.of(2026, 6, 6, 1, 0),
                        LocalDateTime.of(2026, 6, 13, 1, 0),
                        "{\"repository\":\"s3-prod\"}");
        when(service.recordOperatorEvidence(8L, command, "operator-1"))
                .thenReturn(evidence("backup_restore", "PASS"));
        CdpWarehouseEnterpriseOlapEvidenceController controller =
                new CdpWarehouseEnterpriseOlapEvidenceController(service, resolver);

        StepVerifier.create(controller.record(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().tenantId()).isEqualTo(8L);
                    assertThat(response.getData().evidenceKey()).isEqualTo("backup_restore");
                })
                .verifyComplete();

        verify(service).recordOperatorEvidence(8L, command, "operator-1");
    }

    @Test
    void latestEvidenceUsesTenantFromContext() {
        CdpWarehouseEnterpriseOlapEvidenceService service =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        TenantContextResolver resolver = resolver();
        when(service.latestEvidence(8L)).thenReturn(new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle(
                8L,
                "PASS",
                LocalDateTime.of(2026, 6, 6, 1, 0),
                List.of(evidence("doris_metrics", "PASS"))));
        CdpWarehouseEnterpriseOlapEvidenceController controller =
                new CdpWarehouseEnterpriseOlapEvidenceController(service, resolver);

        StepVerifier.create(controller.latest())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().tenantId()).isEqualTo(8L);
                    assertThat(response.getData().evidence()).singleElement()
                            .satisfies(row -> assertThat(row.evidenceKey()).isEqualTo("doris_metrics"));
                })
                .verifyComplete();

        verify(service).latestEvidence(8L);
    }

    @Test
    void proofEvidenceUsesTenantFromContext() {
        CdpWarehouseEnterpriseOlapEvidenceService service =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        TenantContextResolver resolver = resolver();
        when(service.proofEvidence(8L)).thenReturn(List.of(
                new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "enterprise_olap:doris_metrics",
                        "PASS",
                        "Doris metrics are fresh")));
        CdpWarehouseEnterpriseOlapEvidenceController controller =
                new CdpWarehouseEnterpriseOlapEvidenceController(service, resolver);

        StepVerifier.create(controller.proof())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement()
                            .satisfies(row -> {
                                assertThat(row.key()).isEqualTo("enterprise_olap:doris_metrics");
                                assertThat(row.status()).isEqualTo("PASS");
                            });
                })
                .verifyComplete();

        verify(service).proofEvidence(8L);
    }

    @Test
    void collectEvidenceUsesTenantAndActorFromContext() {
        CdpWarehouseEnterpriseOlapEvidenceService evidenceService =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        TenantContextResolver resolver = resolver();
        when(collectionService.run(8L, "MANUAL", "operator-1"))
                .thenReturn(collectionRun("PASS"));
        CdpWarehouseEnterpriseOlapEvidenceController controller =
                new CdpWarehouseEnterpriseOlapEvidenceController(evidenceService, collectionService, resolver);

        StepVerifier.create(controller.collect())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().tenantId()).isEqualTo(8L);
                    assertThat(response.getData().status()).isEqualTo("PASS");
                })
                .verifyComplete();

        verify(collectionService).run(8L, "MANUAL", "operator-1");
    }

    @Test
    void collectionHistoryUsesTenantFromContext() {
        CdpWarehouseEnterpriseOlapEvidenceService evidenceService =
                mock(CdpWarehouseEnterpriseOlapEvidenceService.class);
        CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService =
                mock(CdpWarehouseEnterpriseOlapEvidenceCollectionService.class);
        TenantContextResolver resolver = resolver();
        when(collectionService.recentRuns(8L, 20)).thenReturn(List.of(collectionRun("WARN")));
        CdpWarehouseEnterpriseOlapEvidenceController controller =
                new CdpWarehouseEnterpriseOlapEvidenceController(evidenceService, collectionService, resolver);

        StepVerifier.create(controller.collections(20))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement()
                            .satisfies(row -> assertThat(row.status()).isEqualTo("WARN"));
                })
                .verifyComplete();

        verify(collectionService).recentRuns(8L, 20);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(
                new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView evidence(String key, String status) {
        return new CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView(
                1L,
                8L,
                key,
                "operator",
                status,
                key + " " + status,
                LocalDateTime.of(2026, 6, 6, 1, 0),
                LocalDateTime.of(2026, 6, 13, 1, 0),
                "{}",
                "operator-1");
    }

    private CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView collectionRun(String status) {
        return new CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView(
                501L,
                8L,
                "MANUAL",
                status,
                LocalDateTime.of(2026, 6, 6, 1, 0),
                LocalDateTime.of(2026, 6, 6, 1, 1),
                4,
                3,
                "WARN".equals(status) ? 1 : 0,
                "FAIL".equals(status) ? 1 : 0,
                "collection " + status,
                "operator-1");
    }
}
