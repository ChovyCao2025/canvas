package org.chovy.canvas.domain.bi.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetExtractRefreshRunDO;
import org.chovy.canvas.dal.dataobject.BiQuickEngineCapacityPolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetExtractRefreshRunMapper;
import org.chovy.canvas.dal.mapper.BiQuickEngineCapacityPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQuickEngineCapacityServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void summarizesTenantQuickEngineCapacityAcrossExtractDatasetsAndUsers() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of(
                successRun(91L, "canvas_daily_stats", "bi_extract.t7_canvas_daily_stats_20260606101530", 42_000L,
                        "alice", LocalDateTime.of(2026, 6, 6, 10, 15, 30), "ACTIVE"),
                successRun(90L, "canvas_daily_stats", "bi_extract.t7_canvas_daily_stats_20260606091530", 41_000L,
                        "alice", LocalDateTime.of(2026, 6, 6, 9, 15, 30), null),
                successRun(89L, "canvas_daily_stats", "bi_extract.t7_canvas_daily_stats_20260606081530", 40_000L,
                        "alice", LocalDateTime.of(2026, 6, 6, 8, 15, 30), "DROPPED"),
                successRun(88L, "node_daily_stats", "bi_extract.t7_node_daily_stats_20260606100000", 90_000L,
                        "bob", LocalDateTime.of(2026, 6, 6, 10, 0, 0), "ACTIVE"),
                failedRun(87L, "campaign_sql", "ops", LocalDateTime.of(2026, 6, 6, 7, 0, 0))));
        BiQuickEngineCapacityService service = service(policyMapper, runMapper, mock(BiAuditLogMapper.class));

        BiQuickEngineCapacitySummaryView summary = service.summary(7L, 10);

        assertThat(summary.tenantId()).isEqualTo(7L);
        assertThat(summary.capacityLimitRows()).isEqualTo(200_000L);
        assertThat(summary.usedRows()).isEqualTo(173_000L);
        assertThat(summary.usagePercent()).isEqualTo(86.5);
        assertThat(summary.alertLevel()).isEqualTo("WARNING");
        assertThat(summary.categories()).singleElement().satisfies(category -> {
            assertThat(category.type()).isEqualTo("DATASET_ACCELERATION");
            assertThat(category.usedRows()).isEqualTo(173_000L);
            assertThat(category.resourceCount()).isEqualTo(2);
        });
        assertThat(summary.details()).hasSize(2);
        assertThat(summary.details().get(0).resourceKey()).isEqualTo("node_daily_stats");
        assertThat(summary.details().get(0).usedRows()).isEqualTo(90_000L);
        assertThat(summary.details().get(1).resourceKey()).isEqualTo("canvas_daily_stats");
        assertThat(summary.details().get(1).activeTables()).isEqualTo(2);
        assertThat(summary.userRankings()).extracting(BiQuickEngineCapacityUserUsageView::user)
                .containsExactly("bob", "alice");
    }

    @Test
    void upsertsCapacityAlertPolicyAndWritesAuditSnapshot() throws Exception {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        BiQuickEngineCapacityPolicyDO existing = policy(false, 100_000L, 70, 90);
        existing.setId(31L);
        BiQuickEngineCapacityPolicyDO updated = policy(true, 500_000L, 75, 95);
        updated.setId(31L);
        updated.setNotificationChannels("[\"LARK\",\"EMAIL\"]");
        updated.setNotificationReceivers("[\"bi-ops\",\"alice\"]");
        updated.setUpdatedBy("alice");
        updated.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 12, 0, 0));
        when(policyMapper.selectList(any())).thenReturn(List.of(existing), List.of(updated));
        BiQuickEngineCapacityService service = service(policyMapper, runMapper, auditLogMapper);

        BiQuickEngineCapacityAlertPolicyView view = service.upsertAlertPolicy(
                7L,
                new BiQuickEngineCapacityAlertPolicyCommand(
                        true,
                        500_000L,
                        75,
                        95,
                        List.of("LARK", "EMAIL"),
                        List.of("bi-ops", "alice")),
                "alice");

        ArgumentCaptor<BiQuickEngineCapacityPolicyDO> saved =
                ArgumentCaptor.forClass(BiQuickEngineCapacityPolicyDO.class);
        verify(policyMapper).updateById(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(31L);
        assertThat(saved.getValue().getTenantId()).isEqualTo(7L);
        assertThat(saved.getValue().getEnabled()).isTrue();
        assertThat(saved.getValue().getCapacityLimitRows()).isEqualTo(500_000L);
        assertThat(saved.getValue().getWarningThresholdPercent()).isEqualTo(75);
        assertThat(saved.getValue().getCriticalThresholdPercent()).isEqualTo(95);
        assertThat(saved.getValue().getNotificationChannels()).isEqualTo("[\"LARK\",\"EMAIL\"]");
        assertThat(view.notificationReceivers()).containsExactly("bi-ops", "alice");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_QUICK_ENGINE_CAPACITY_POLICY_UPDATE");
        JsonNode detail = new ObjectMapper().readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("before").path("enabled").asBoolean()).isFalse();
        assertThat(detail.path("after").path("capacityLimitRows").asLong()).isEqualTo(500_000L);
    }

    @Test
    void rejectsCapacityAlertPolicyWithTooManyNotificationChannels() {
        BiQuickEngineCapacityService service = service(
                mock(BiQuickEngineCapacityPolicyMapper.class),
                mock(BiDatasetExtractRefreshRunMapper.class),
                mock(BiAuditLogMapper.class));

        assertThatThrownBy(() -> service.upsertAlertPolicy(
                7L,
                new BiQuickEngineCapacityAlertPolicyCommand(
                        true,
                        500_000L,
                        80,
                        95,
                        List.of("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10"),
                        List.of("bi-ops")),
                "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notification channels");
    }

    @Test
    void summarizesTenantPoolConcurrencyAndQueueTelemetry() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(4);
        policy.setQueueLimit(10);
        policy.setQueueTimeoutSeconds(180);
        policy.setPoolWeight(200);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiQueryHistoryReader historyReader = (tenantId, limit) -> List.of(
                history(1L, "sales", "alice", "RUNNING", 600L),
                history(2L, "sales", "bob", "RUNNING", 400L),
                history(3L, "sales", "carol", "QUEUED", 0L),
                history(4L, "sales", "dave", "BLOCKED", 20L),
                history(5L, "sales", "erin", "SUCCESS", 120L),
                history(6L, "sales", "frank", "CACHE_HIT", 5L));
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                historyReader);

        BiQuickEngineCapacitySummaryView summary = service.summary(7L, 20);

        assertThat(summary.tenantPoolPolicy().poolKey()).isEqualTo("GOLD");
        assertThat(summary.tenantPoolPolicy().maxConcurrentQueries()).isEqualTo(4);
        assertThat(summary.tenantPoolPolicy().queueLimit()).isEqualTo(10);
        assertThat(summary.tenantPoolPolicy().queueTimeoutSeconds()).isEqualTo(180);
        assertThat(summary.tenantPoolPolicy().poolWeight()).isEqualTo(200);
        assertThat(summary.concurrencyQueue().runningQueries()).isEqualTo(2);
        assertThat(summary.concurrencyQueue().queuedQueries()).isEqualTo(1);
        assertThat(summary.concurrencyQueue().blockedQueries()).isEqualTo(1);
        assertThat(summary.concurrencyQueue().successfulQueries()).isEqualTo(2);
        assertThat(summary.concurrencyQueue().concurrencyUsagePercent()).isEqualTo(50.0);
        assertThat(summary.concurrencyQueue().queueUsagePercent()).isEqualTo(10.0);
        assertThat(summary.concurrencyQueue().state()).isEqualTo("NORMAL");
    }

    @Test
    void admitsOnlyUpToLiveTenantPoolConcurrencyAndReleasesSlots() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(1);
        policy.setQueueLimit(10);
        policy.setQueueTimeoutSeconds(180);
        policy.setPoolWeight(200);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                (tenantId, limit) -> List.of());

        BiQuickEngineAdmissionDecision first = service.admitQuery(7L, 20);
        BiQuickEngineAdmissionDecision second = service.admitQuery(7L, 20);
        service.releaseQuery(7L);
        BiQuickEngineAdmissionDecision third = service.admitQuery(7L, 20);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isFalse();
        assertThat(second.message()).contains("max concurrent");
        assertThat(third.allowed()).isTrue();
    }

    @Test
    void waitsForTenantPoolSlotWhenQueueHasCapacityAndReleaseWakesIt() throws Exception {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(1);
        policy.setQueueLimit(2);
        policy.setQueueTimeoutSeconds(2);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                (tenantId, limit) -> List.of());

        BiQuickEngineAdmissionDecision first = service.admitQuery(7L, 20);
        CompletableFuture<BiQuickEngineAdmissionDecision> queued = CompletableFuture.supplyAsync(
                () -> service.admitQueryOrWait(7L, 20));

        Thread.sleep(100L);
        assertThat(first.allowed()).isTrue();
        assertThat(queued).isNotDone();

        service.releaseQuery(7L);
        BiQuickEngineAdmissionDecision admitted = queued.get(1, TimeUnit.SECONDS);

        assertThat(admitted.allowed()).isTrue();
        assertThat(admitted.status()).isEqualTo("ADMITTED_AFTER_QUEUE");
        assertThat(admitted.message()).contains("queued").contains("GOLD");

        service.releaseQuery(7L);
    }

    @Test
    void timesOutQueuedTenantPoolAdmissionWhenNoSlotIsReleased() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(1);
        policy.setQueueLimit(1);
        policy.setQueueTimeoutSeconds(1);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                (tenantId, limit) -> List.of());

        BiQuickEngineAdmissionDecision first = service.admitQuery(7L, 20);
        BiQuickEngineAdmissionDecision timedOut = service.admitQueryOrWait(7L, 20);
        BiQuickEngineCapacitySummaryView summary = service.summary(7L, 20);

        assertThat(first.allowed()).isTrue();
        assertThat(timedOut.allowed()).isFalse();
        assertThat(timedOut.status()).isEqualTo("BLOCKED");
        assertThat(timedOut.message()).contains("queue wait timed out").contains("GOLD");
        assertThat(summary.concurrencyQueue().queuedQueries()).isZero();

        service.releaseQuery(7L);
    }

    @Test
    void interruptsQueuedTenantPoolAdmissionAndClearsQueueSlot() throws Exception {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(1);
        policy.setQueueLimit(1);
        policy.setQueueTimeoutSeconds(5);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                (tenantId, limit) -> List.of());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        BiQuickEngineAdmissionDecision first = service.admitQuery(7L, 20);
        Future<BiQuickEngineAdmissionDecision> waiting = executor.submit(() -> service.admitQueryOrWait(7L, 20));
        Thread.sleep(100L);
        executor.shutdownNow();
        BiQuickEngineAdmissionDecision interrupted = waiting.get(1, TimeUnit.SECONDS);
        BiQuickEngineCapacitySummaryView summary = service.summary(7L, 20);

        assertThat(first.allowed()).isTrue();
        assertThat(interrupted.allowed()).isFalse();
        assertThat(interrupted.status()).isEqualTo("BLOCKED");
        assertThat(interrupted.message()).contains("queue wait interrupted").contains("GOLD");
        assertThat(summary.concurrencyQueue().queuedQueries()).isZero();

        service.releaseQuery(7L);
    }

    @Test
    void acquiresAndReleasesDistributedTenantPoolSlotLeaseWhenConfigured() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(2);
        policy.setQueueLimit(10);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_POOL_GOLD_SLOT_0", Duration.ofSeconds(300)))
                .thenReturn(false);
        when(leaseService.acquire(7L, "BI_QUICK_ENGINE_POOL_GOLD_SLOT_1", Duration.ofSeconds(300)))
                .thenReturn(true);
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                (tenantId, limit) -> List.of(),
                leaseService,
                300L);

        BiQuickEngineAdmissionDecision decision = service.admitQuery(7L, 20);
        service.releaseQuery(7L);

        assertThat(decision.allowed()).isTrue();
        verify(leaseService).acquire(7L, "BI_QUICK_ENGINE_POOL_GOLD_SLOT_0", Duration.ofSeconds(300));
        verify(leaseService).acquire(7L, "BI_QUICK_ENGINE_POOL_GOLD_SLOT_1", Duration.ofSeconds(300));
        verify(leaseService).release(7L, "BI_QUICK_ENGINE_POOL_GOLD_SLOT_1");
    }

    @Test
    void upsertsTenantPoolPolicyAndWritesAuditSnapshot() throws Exception {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        BiQuickEngineCapacityPolicyDO existing = policy(false, 100_000L, 70, 90);
        existing.setId(31L);
        existing.setPoolKey("STANDARD");
        existing.setMaxConcurrentQueries(8);
        existing.setQueueLimit(50);
        existing.setQueueTimeoutSeconds(120);
        existing.setPoolWeight(100);
        BiQuickEngineCapacityPolicyDO updated = policy(false, 100_000L, 70, 90);
        updated.setId(31L);
        updated.setPoolKey("GOLD");
        updated.setMaxConcurrentQueries(16);
        updated.setQueueLimit(120);
        updated.setQueueTimeoutSeconds(300);
        updated.setPoolWeight(200);
        updated.setUpdatedBy("alice");
        updated.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 12, 0, 0));
        when(policyMapper.selectList(any())).thenReturn(List.of(existing), List.of(updated));
        BiQuickEngineCapacityService service = service(policyMapper, runMapper, auditLogMapper);

        BiQuickEngineTenantPoolPolicyView view = service.upsertTenantPoolPolicy(
                7L,
                new BiQuickEngineTenantPoolPolicyCommand("gold", 16, 120, 300, 200),
                "alice");

        ArgumentCaptor<BiQuickEngineCapacityPolicyDO> saved =
                ArgumentCaptor.forClass(BiQuickEngineCapacityPolicyDO.class);
        verify(policyMapper).updateById(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(7L);
        assertThat(saved.getValue().getPoolKey()).isEqualTo("GOLD");
        assertThat(saved.getValue().getMaxConcurrentQueries()).isEqualTo(16);
        assertThat(saved.getValue().getQueueLimit()).isEqualTo(120);
        assertThat(saved.getValue().getQueueTimeoutSeconds()).isEqualTo(300);
        assertThat(saved.getValue().getPoolWeight()).isEqualTo(200);
        assertThat(view.poolKey()).isEqualTo("GOLD");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_QUICK_ENGINE_TENANT_POOL_POLICY_UPDATE");
        JsonNode detail = new ObjectMapper().readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("before").path("poolKey").asText()).isEqualTo("STANDARD");
        assertThat(detail.path("after").path("maxConcurrentQueries").asInt()).isEqualTo(16);
    }

    @Test
    void deniesQueryAdmissionWhenTenantPoolConcurrencyIsSaturated() {
        BiQuickEngineCapacityPolicyMapper policyMapper = mock(BiQuickEngineCapacityPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiQuickEngineCapacityPolicyDO policy = policy(true, 200_000L, 80, 95);
        policy.setPoolKey("GOLD");
        policy.setMaxConcurrentQueries(2);
        policy.setQueueLimit(10);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        BiQueryHistoryReader historyReader = (tenantId, limit) -> List.of(
                history(1L, "sales", "alice", "RUNNING", 600L),
                history(2L, "sales", "bob", "RUNNING", 400L));
        BiQuickEngineCapacityService service = new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                new ObjectMapper(),
                CLOCK,
                1_000_000L,
                historyReader);

        BiQuickEngineAdmissionDecision decision = service.admitQuery(7L, 20);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.status()).isEqualTo("BLOCKED");
        assertThat(decision.message()).contains("GOLD").contains("max concurrent");
        assertThat(decision.tenantPoolPolicy().maxConcurrentQueries()).isEqualTo(2);
        assertThat(decision.concurrencyQueue().runningQueries()).isEqualTo(2);
    }

    private BiQuickEngineCapacityService service(BiQuickEngineCapacityPolicyMapper policyMapper,
                                                 BiDatasetExtractRefreshRunMapper runMapper,
                                                 BiAuditLogMapper auditLogMapper) {
        return new BiQuickEngineCapacityService(
                policyMapper,
                runMapper,
                auditLogMapper,
                new ObjectMapper(),
                CLOCK,
                1_000_000L);
    }

    private BiQuickEngineCapacityPolicyDO policy(Boolean enabled,
                                                 Long limitRows,
                                                 Integer warningPercent,
                                                 Integer criticalPercent) {
        BiQuickEngineCapacityPolicyDO row = new BiQuickEngineCapacityPolicyDO();
        row.setTenantId(7L);
        row.setEnabled(enabled);
        row.setCapacityLimitRows(limitRows);
        row.setWarningThresholdPercent(warningPercent);
        row.setCriticalThresholdPercent(criticalPercent);
        row.setNotificationChannels("[\"EMAIL\"]");
        row.setNotificationReceivers("[\"bi-ops\"]");
        row.setUpdatedBy("ops");
        return row;
    }

    private BiDatasetExtractRefreshRunDO successRun(Long id,
                                                    String datasetKey,
                                                    String materializedTable,
                                                    Long rowCount,
                                                    String requestedBy,
                                                    LocalDateTime finishedAt,
                                                    String retentionStatus) {
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetKey(datasetKey);
        row.setStatus("SUCCESS");
        row.setRowCount(rowCount);
        row.setDurationMs(137L);
        row.setMaterializedTable(materializedTable);
        row.setRequestedBy(requestedBy);
        row.setStartedAt(finishedAt.minusSeconds(1));
        row.setFinishedAt(finishedAt);
        row.setRetentionStatus(retentionStatus);
        return row;
    }

    private BiDatasetExtractRefreshRunDO failedRun(Long id,
                                                   String datasetKey,
                                                   String requestedBy,
                                                   LocalDateTime finishedAt) {
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetKey(datasetKey);
        row.setStatus("FAILED");
        row.setRequestedBy(requestedBy);
        row.setStartedAt(finishedAt.minusSeconds(1));
        row.setFinishedAt(finishedAt);
        row.setErrorMessage("warehouse unavailable");
        return row;
    }

    private BiQueryHistoryItem history(Long id,
                                       String datasetKey,
                                       String username,
                                       String status,
                                       long durationMs) {
        return new BiQueryHistoryItem(
                id,
                datasetKey,
                username,
                100,
                durationMs,
                status,
                "hash-" + id,
                null,
                LocalDateTime.of(2026, 6, 6, 12, 0, 0).minusMinutes(id));
    }
}
