package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessEvidence;
import org.chovy.canvas.cdp.domain.CdpWarehouseReadinessRepository;
import org.chovy.canvas.cdp.domain.WarehouseBiDatasource;
import org.chovy.canvas.cdp.domain.WarehouseIncident;
import org.chovy.canvas.cdp.domain.WarehouseMaterializationRun;
import org.chovy.canvas.cdp.domain.WarehouseRealtimeStatus;
import org.chovy.canvas.cdp.domain.WarehouseSyncRun;
import org.chovy.canvas.cdp.domain.WarehouseWatermark;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 CdpWarehouseReadinessApplicationService 的核心行为。
 */
class CdpWarehouseReadinessApplicationServiceTest {

    /**
     * 执行 fixed 对应的 CDP 业务操作。
     */
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            /**
             * 执行 of 对应的 CDP 业务操作。
             */
            ZoneId.of("Asia/Shanghai"));

    /**
     * 执行 allHealthyEvidenceProducesPassAcrossRequiredSections 对应的 CDP 业务操作。
     */
    @Test
    void allHealthyEvidenceProducesPassAcrossRequiredSections() {
        CdpWarehouseReadinessApplicationService service = new CdpWarehouseReadinessApplicationService(
                tenantId -> healthyEvidence(tenantId),
                CLOCK);

        CdpWarehouseReadinessView view = service.readiness(9L);

        assertThat(view.tenantId()).isEqualTo(9L);
        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.sections()).extracting("key")
                .containsExactly("offline_sync", "realtime_pipelines", "incidents", "bi_datasources",
                        "audience_materialization");
        assertThat(view.sections()).extracting("status")
                .containsExactly("PASS", "PASS", "PASS", "PASS", "PASS");
    }

    /**
     * 执行 criticalIncidentFailsOverallReadiness 对应的 CDP 业务操作。
     */
    @Test
    void criticalIncidentFailsOverallReadiness() {
        CdpWarehouseReadinessApplicationService service = new CdpWarehouseReadinessApplicationService(
                tenantId -> healthyEvidence(tenantId).withIncidents(List.of(new WarehouseIncident("CRITICAL", "OPEN"))),
                CLOCK);

        CdpWarehouseReadinessView view = service.readiness(9L);

        assertThat(view.status()).isEqualTo("FAIL");
        assertThat(view.sections()).filteredOn(section -> "incidents".equals(section.key()))
                .singleElement()
                .satisfies(section -> assertThat(section.reason()).contains("critical open warehouse incident"));
    }

    /**
     * 执行 missingEvidenceWarnsInsteadOfPassing 对应的 CDP 业务操作。
     */
    @Test
    void missingEvidenceWarnsInsteadOfPassing() {
        CdpWarehouseReadinessRepository repository = tenantId -> new CdpWarehouseReadinessEvidence(
                tenantId,
                List.of(),
                List.of(),
                new WarehouseRealtimeStatus(0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of());
        CdpWarehouseReadinessApplicationService service = new CdpWarehouseReadinessApplicationService(repository, CLOCK);

        CdpWarehouseReadinessView view = service.readiness(null);

        assertThat(view.tenantId()).isZero();
        assertThat(view.status()).isEqualTo("WARN");
        assertThat(view.sections()).extracting("status")
                .contains("WARN");
    }

    /**
     * 执行 healthyEvidence 对应的 CDP 业务操作。
     */
    private static CdpWarehouseReadinessEvidence healthyEvidence(Long tenantId) {
        LocalDateTime recent = LocalDateTime.parse("2026-06-06T09:55:00");
        return new CdpWarehouseReadinessEvidence(
                tenantId,
                List.of(new WarehouseSyncRun("SUCCESS", recent, recent, recent, recent)),
                List.of(new WarehouseWatermark(recent, recent)),
                new WarehouseRealtimeStatus(2, 2, 0, 0),
                List.of(),
                List.of(new WarehouseBiDatasource(true)),
                List.of(new WarehouseMaterializationRun("SUCCESS", recent, recent)));
    }
}
