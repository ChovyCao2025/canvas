package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.dal.dataobject.BiDatasetAccelerationPolicyDO;
import org.chovy.canvas.dal.mapper.BiDatasetAccelerationPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.domain.bi.query.BiQueryCachePolicyService;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BiDatasetAccelerationSchedulerServiceTest {

    @Test
    void runDueOnceRefreshesDueScheduledExtractPolicyAndInvalidatesDatasetCache() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 5);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy("canvas_daily_stats", true, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L),
                policy("fresh_dataset", true, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 9, 45), 60L),
                policy("manual_dataset", true, "EXTRACT", "MANUAL",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L),
                policy("cache_dataset", true, "CACHE", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L),
                policy("disabled_dataset", false, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L)));
        when(accelerationService.refreshNow(7L, "canvas_daily_stats", "scheduler"))
                .thenReturn(new BiDatasetExtractRefreshRunView(
                        91L,
                        "canvas_daily_stats",
                        "SUCCESS",
                        42_000L,
                        137L,
                        "bi_extract.t7_canvas_daily_stats_20260606100500",
                        "scheduler",
                        now.minusSeconds(2),
                        now,
                        null));
        BiDatasetAccelerationSchedulerService service = service(
                policyMapper,
                accelerationService,
                cachePolicyService,
                true,
                null);

        BiDatasetAccelerationSchedulerResult result = service.runDueOnce(7L, "scheduler", now);

        assertThat(result.policiesChecked()).isEqualTo(5);
        assertThat(result.refreshed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(4);
        assertThat(result.failed()).isZero();
        verify(accelerationService).refreshNow(7L, "canvas_daily_stats", "scheduler");
        ArgumentCaptor<BiQueryCacheInvalidationCommand> invalidation =
                ArgumentCaptor.forClass(BiQueryCacheInvalidationCommand.class);
        verify(cachePolicyService).invalidate(invalidation.capture());
        assertThat(invalidation.getValue().scope()).isEqualTo("DATASET");
        assertThat(invalidation.getValue().datasetKey()).isEqualTo("canvas_daily_stats");
    }

    @Test
    void runDueOnceCountsRefreshFailuresWithoutInvalidatingCache() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 5);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy(
                "canvas_daily_stats",
                true,
                "EXTRACT",
                "SCHEDULED",
                LocalDateTime.of(2026, 6, 6, 8, 55),
                60L)));
        when(accelerationService.refreshNow(7L, "canvas_daily_stats", "scheduler"))
                .thenThrow(new IllegalStateException("warehouse unavailable"));
        BiDatasetAccelerationSchedulerService service = service(
                policyMapper,
                accelerationService,
                cachePolicyService,
                true,
                null);

        BiDatasetAccelerationSchedulerResult result = service.runDueOnce(7L, "scheduler", now);

        assertThat(result.policiesChecked()).isEqualTo(1);
        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        verify(cachePolicyService, never()).invalidate(any());
    }

    @Test
    void runDueOnceReturnsPerPolicyObservabilityForApiExtractSchedules() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 5);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy("orders_api_extract", true, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L),
                policy("fresh_api_extract", true, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 9, 45), 60L),
                policy("broken_api_extract", true, "EXTRACT", "SCHEDULED",
                        LocalDateTime.of(2026, 6, 6, 8, 55), 60L)));
        when(accelerationService.refreshNow(7L, "orders_api_extract", "scheduler"))
                .thenReturn(new BiDatasetExtractRefreshRunView(
                        91L,
                        "orders_api_extract",
                        "SUCCESS",
                        42_000L,
                        137L,
                        "bi_extract.t7_orders_api_extract_20260606100500",
                        "scheduler",
                        now.minusSeconds(2),
                        now,
                        null));
        when(accelerationService.refreshNow(7L, "broken_api_extract", "scheduler"))
                .thenThrow(new IllegalStateException("API endpoint timeout"));
        BiDatasetAccelerationSchedulerService service = service(
                policyMapper,
                accelerationService,
                cachePolicyService,
                true,
                null);

        BiDatasetAccelerationSchedulerResult result = service.runDueOnce(7L, "scheduler", now);

        assertThat(result.policiesChecked()).isEqualTo(3);
        assertThat(result.refreshed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.items()).extracting(BiDatasetAccelerationSchedulerItem::datasetKey)
                .containsExactly("orders_api_extract", "fresh_api_extract", "broken_api_extract");
        assertThat(result.items()).extracting(BiDatasetAccelerationSchedulerItem::status)
                .containsExactly("REFRESHED", "SKIPPED", "FAILED");
        assertThat(result.items().get(0).refreshRunId()).isEqualTo(91L);
        assertThat(result.items().get(0).rowCount()).isEqualTo(42_000L);
        assertThat(result.items().get(0).materializedTable())
                .isEqualTo("bi_extract.t7_orders_api_extract_20260606100500");
        assertThat(result.items().get(1).reason()).isEqualTo("not due");
        assertThat(result.items().get(2).reason()).contains("API endpoint timeout");
    }

    @Test
    void runScheduledOnceUsesDistributedLease() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 5);
        when(leaseService.acquire(7L, "BI_DATASET_ACCELERATION_SCHEDULER", Duration.ofSeconds(120)))
                .thenReturn(true);
        when(policyMapper.selectList(any())).thenReturn(List.of());
        BiDatasetAccelerationSchedulerService service = service(
                policyMapper,
                accelerationService,
                cachePolicyService,
                true,
                leaseService);

        BiDatasetAccelerationSchedulerResult result = service.runScheduledOnce(now);

        assertThat(result.policiesChecked()).isZero();
        assertThat(result.skipped()).isZero();
        verify(leaseService).acquire(7L, "BI_DATASET_ACCELERATION_SCHEDULER", Duration.ofSeconds(120));
        verify(leaseService).release(7L, "BI_DATASET_ACCELERATION_SCHEDULER");
    }

    @Test
    void runScheduledOnceSkipsWhenLeaseIsHeldByAnotherInstance() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetAccelerationService accelerationService = mock(BiDatasetAccelerationService.class);
        BiQueryCachePolicyService cachePolicyService = mock(BiQueryCachePolicyService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_DATASET_ACCELERATION_SCHEDULER", Duration.ofSeconds(120)))
                .thenReturn(false);
        BiDatasetAccelerationSchedulerService service = service(
                policyMapper,
                accelerationService,
                cachePolicyService,
                true,
                leaseService);

        BiDatasetAccelerationSchedulerResult result =
                service.runScheduledOnce(LocalDateTime.of(2026, 6, 6, 10, 5));

        assertThat(result.policiesChecked()).isZero();
        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(leaseService).acquire(7L, "BI_DATASET_ACCELERATION_SCHEDULER", Duration.ofSeconds(120));
        verify(leaseService, never()).release(7L, "BI_DATASET_ACCELERATION_SCHEDULER");
        verifyNoInteractions(policyMapper, accelerationService, cachePolicyService);
    }

    @Test
    void runScheduledOnceReturnsEmptyWhenDisabled() {
        BiDatasetAccelerationSchedulerService service = service(
                mock(BiDatasetAccelerationPolicyMapper.class),
                mock(BiDatasetAccelerationService.class),
                mock(BiQueryCachePolicyService.class),
                false,
                null);

        BiDatasetAccelerationSchedulerResult result =
                service.runScheduledOnce(LocalDateTime.of(2026, 6, 6, 10, 5));

        assertThat(result.policiesChecked()).isZero();
        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
    }

    private BiDatasetAccelerationSchedulerService service(BiDatasetAccelerationPolicyMapper policyMapper,
                                                          BiDatasetAccelerationService accelerationService,
                                                          BiQueryCachePolicyService cachePolicyService,
                                                          boolean enabled,
                                                          BiDeliverySchedulerLeaseService leaseService) {
        return new BiDatasetAccelerationSchedulerService(
                policyMapper,
                accelerationService,
                cachePolicyService,
                enabled,
                7L,
                "scheduler",
                50,
                leaseService,
                120);
    }

    private BiDatasetAccelerationPolicyDO policy(String datasetKey,
                                                 Boolean enabled,
                                                 String accelerationMode,
                                                 String refreshMode,
                                                 LocalDateTime lastRefreshedAt,
                                                 Long intervalMinutes) {
        BiDatasetAccelerationPolicyDO row = new BiDatasetAccelerationPolicyDO();
        row.setId((long) datasetKey.hashCode());
        row.setTenantId(7L);
        row.setDatasetKey(datasetKey);
        row.setEnabled(enabled);
        row.setAccelerationMode(accelerationMode);
        row.setRefreshMode(refreshMode);
        row.setRefreshIntervalMinutes(intervalMinutes);
        row.setTtlSeconds(300L);
        row.setMaxRows(100_000L);
        row.setLastRefreshedAt(lastRefreshedAt);
        row.setUpdatedBy("ops");
        return row;
    }
}
