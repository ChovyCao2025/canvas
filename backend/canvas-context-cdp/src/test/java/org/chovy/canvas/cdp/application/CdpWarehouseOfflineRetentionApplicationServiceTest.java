package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade;
import org.junit.jupiter.api.Test;

class CdpWarehouseOfflineRetentionApplicationServiceTest {

    @Test
    void offlineCyclePlanAndRunNormalizeLimitsWindowsAndOperator() {
        CdpWarehouseOfflineRetentionFacade service = new CdpWarehouseOfflineRetentionApplicationService();
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 7, 0);

        CdpWarehouseOfflineRetentionFacade.OfflineCyclePlanView plan =
                service.offlineCyclePlan(9L, now, 1000, 30);
        CdpWarehouseOfflineRetentionFacade.OfflineCycleResultView result =
                service.runOfflineCycle(9L, now, 1000, 30, " alice ");

        assertThat(plan.tenantId()).isEqualTo(9L);
        assertThat(plan.backfillLimit()).isEqualTo(1000);
        assertThat(plan.aggregationWindowMinutes()).isEqualTo(30);
        assertThat(plan.steps()).extracting(CdpWarehouseOfflineRetentionFacade.OfflineCycleStepPlanView::stepKey)
                .containsExactly("BACKFILL", "AGGREGATE");
        assertThat(plan.steps().get(0).status()).isEqualTo("READY");
        assertThat(plan.steps().get(1).status()).isEqualTo("WAITING_FOR_BACKFILL");

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.operator()).isEqualTo("alice");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.steps()).extracting(CdpWarehouseOfflineRetentionFacade.OfflineCycleStepResultView::stepKey)
                .containsExactly("BACKFILL", "AGGREGATE");
        assertThat(result.loadedRows()).isGreaterThan(0L);
        assertThat(result.failedRows()).isZero();

        assertThatThrownBy(() -> service.offlineCyclePlan(9L, now, 6000, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("backfillLimit must be between 1 and 5000");
        assertThatThrownBy(() -> service.offlineCyclePlan(9L, now, 1000, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregationWindowMinutes must be positive");
    }

    @Test
    void retentionPlanAndRunComputeCutoffsEligibleRowsAndOperatorDefaults() {
        CdpWarehouseOfflineRetentionFacade service = new CdpWarehouseOfflineRetentionApplicationService();
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 7, 0);

        CdpWarehouseOfflineRetentionFacade.RetentionPlanView plan =
                service.retentionPlan(0L, now, 30, 14, 90);
        CdpWarehouseOfflineRetentionFacade.RetentionCleanupResultView cleanup =
                service.runRetention(0L, now, 30, 14, 90, null);

        assertThat(plan.tenantId()).isZero();
        assertThat(plan.syncRuns().cutoff()).isEqualTo(now.minusDays(30));
        assertThat(plan.realtimeRetries().cutoff()).isEqualTo(now.minusDays(14));
        assertThat(plan.resolvedIncidents().cutoff()).isEqualTo(now.minusDays(90));
        assertThat(plan.totalEligibleRows()).isEqualTo(6L);

        assertThat(cleanup.operator()).isEqualTo("warehouse-retention");
        assertThat(cleanup.totalDeletedRows()).isEqualTo(6L);
        assertThat(cleanup.syncRuns().deletedRows()).isEqualTo(2);
        assertThat(cleanup.realtimeRetries().deletedRows()).isEqualTo(3);
        assertThat(cleanup.resolvedIncidents().deletedRows()).isEqualTo(1);

        assertThatThrownBy(() -> service.retentionPlan(0L, now, 0, 14, 90))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("syncRunRetentionDays must be between 1 and 3650");
    }
}
