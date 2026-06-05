package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BiDeliverySchedulerServiceTest {

    @Test
    void runDueOnceTriggersDueSubscription() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiSubscriptionDO subscription = subscription(
                "{\"frequency\":\"DAILY\",\"time\":\"09:00\"}",
                LocalDateTime.of(2026, 6, 4, 8, 0),
                true);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        when(alertRuleMapper.selectList(any())).thenReturn(List.of());
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of());
        when(runtimeService.runSubscription(7L, 31L, "alice"))
                .thenReturn(new BiDeliveryRunResult("SUBSCRIPTION", 31L, "canvas-daily", "TRIGGERED", List.of()));
        BiDeliverySchedulerService service = service(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService, true);

        BiDeliverySchedulerResult result = service.runDueOnce(7L, "alice", "OPERATOR",
                LocalDateTime.of(2026, 6, 5, 9, 5));

        assertThat(result.subscriptionsChecked()).isEqualTo(1);
        assertThat(result.subscriptionsTriggered()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(runtimeService).runSubscription(7L, 31L, "alice");
    }

    @Test
    void runDueOnceSkipsRecentSubscriptionDelivery() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription(
                "{\"frequency\":\"DAILY\",\"time\":\"09:00\"}",
                LocalDateTime.of(2026, 6, 4, 8, 0),
                true)));
        when(alertRuleMapper.selectList(any())).thenReturn(List.of());
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(log(LocalDateTime.of(2026, 6, 5, 9, 1))));
        BiDeliverySchedulerService service = service(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService, true);

        BiDeliverySchedulerResult result = service.runDueOnce(7L, "alice", "OPERATOR",
                LocalDateTime.of(2026, 6, 5, 9, 5));

        assertThat(result.subscriptionsChecked()).isEqualTo(1);
        assertThat(result.subscriptionsTriggered()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void runDueOnceTriggersAlertByCheckInterval() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of());
        when(alertRuleMapper.selectList(any())).thenReturn(List.of(alert(
                "{\"operator\":\"LT\",\"threshold\":0.9,\"checkIntervalMinutes\":15}",
                LocalDateTime.of(2026, 6, 5, 8, 0),
                true)));
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(log(LocalDateTime.of(2026, 6, 5, 8, 45))));
        when(runtimeService.runAlert(7L, "alice", "OPERATOR", 41L))
                .thenReturn(new BiDeliveryRunResult("ALERT", 41L, "success-rate-alert", "TRIGGERED", List.of()));
        BiDeliverySchedulerService service = service(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService, true);

        BiDeliverySchedulerResult result = service.runDueOnce(7L, "alice", "OPERATOR",
                LocalDateTime.of(2026, 6, 5, 9, 1));

        assertThat(result.alertsChecked()).isEqualTo(1);
        assertThat(result.alertsTriggered()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(runtimeService).runAlert(7L, "alice", "OPERATOR", 41L);
    }

    @Test
    void runDueOnceCountsDisabledAndRuntimeFailures() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(
                subscription("{\"frequency\":\"DAILY\"}", LocalDateTime.of(2026, 6, 5, 8, 0), false)));
        when(alertRuleMapper.selectList(any())).thenReturn(List.of(
                alert("{\"checkIntervalMinutes\":5}", LocalDateTime.of(2026, 6, 5, 8, 0), true)));
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of());
        when(runtimeService.runAlert(7L, "scheduler", "SYSTEM", 41L))
                .thenThrow(new IllegalStateException("query failed"));
        BiDeliverySchedulerService service = service(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService, true);

        BiDeliverySchedulerResult result = service.runDueOnce(7L, "scheduler", "SYSTEM",
                LocalDateTime.of(2026, 6, 5, 9, 1));

        assertThat(result.subscriptionsChecked()).isEqualTo(1);
        assertThat(result.alertsChecked()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    void runScheduledOnceReturnsEmptyWhenDisabled() {
        BiDeliverySchedulerService service = service(
                mock(BiSubscriptionMapper.class),
                mock(BiAlertRuleMapper.class),
                mock(BiDeliveryLogMapper.class),
                mock(BiDeliveryRuntimeService.class),
                false);

        BiDeliverySchedulerResult result = service.runScheduledOnce(LocalDateTime.of(2026, 6, 5, 9, 0));

        assertThat(result.subscriptionsChecked()).isZero();
        assertThat(result.alertsChecked()).isZero();
        assertThat(result.skipped()).isZero();
    }

    @Test
    void runScheduledOnceUsesDistributedLeaseWhenEnabled() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120))).thenReturn(true);
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription(
                "{\"frequency\":\"DAILY\",\"time\":\"09:00\"}",
                LocalDateTime.of(2026, 6, 4, 8, 0),
                true)));
        when(alertRuleMapper.selectList(any())).thenReturn(List.of());
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of());
        when(runtimeService.runSubscription(7L, 31L, "scheduler"))
                .thenReturn(new BiDeliveryRunResult("SUBSCRIPTION", 31L, "canvas-daily", "TRIGGERED", List.of()));
        BiDeliverySchedulerService service = service(
                subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                runtimeService,
                true,
                leaseService);

        BiDeliverySchedulerResult result = service.runScheduledOnce(LocalDateTime.of(2026, 6, 5, 9, 5));

        assertThat(result.subscriptionsTriggered()).isEqualTo(1);
        verify(leaseService).acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120));
        verify(leaseService).release(7L, "BI_DELIVERY_SCHEDULER");
    }

    @Test
    void runScheduledOnceSkipsWhenLeaseIsHeldByAnotherInstance() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliverySchedulerLeaseService leaseService = mock(BiDeliverySchedulerLeaseService.class);
        when(leaseService.acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120))).thenReturn(false);
        BiDeliverySchedulerService service = service(
                subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                runtimeService,
                true,
                leaseService);

        BiDeliverySchedulerResult result = service.runScheduledOnce(LocalDateTime.of(2026, 6, 5, 9, 5));

        assertThat(result.skipped()).isEqualTo(1);
        verify(leaseService).acquire(7L, "BI_DELIVERY_SCHEDULER", Duration.ofSeconds(120));
        verifyNoInteractions(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService);
    }

    private BiDeliverySchedulerService service(BiSubscriptionMapper subscriptionMapper,
                                               BiAlertRuleMapper alertRuleMapper,
                                               BiDeliveryLogMapper deliveryLogMapper,
                                               BiDeliveryRuntimeService runtimeService,
                                               boolean enabled) {
        return service(subscriptionMapper, alertRuleMapper, deliveryLogMapper, runtimeService, enabled, null);
    }

    private BiDeliverySchedulerService service(BiSubscriptionMapper subscriptionMapper,
                                               BiAlertRuleMapper alertRuleMapper,
                                               BiDeliveryLogMapper deliveryLogMapper,
                                               BiDeliveryRuntimeService runtimeService,
                                               boolean enabled,
                                               BiDeliverySchedulerLeaseService leaseService) {
        return new BiDeliverySchedulerService(
                subscriptionMapper,
                alertRuleMapper,
                deliveryLogMapper,
                runtimeService,
                new ObjectMapper(),
                enabled,
                7L,
                "scheduler",
                "SYSTEM",
                50,
                leaseService,
                120);
    }

    private BiSubscriptionDO subscription(String schedule, LocalDateTime createdAt, boolean enabled) {
        BiSubscriptionDO row = new BiSubscriptionDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSubscriptionKey("canvas-daily");
        row.setName("Canvas Daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setScheduleJson(schedule);
        row.setEnabled(enabled);
        row.setCreatedAt(createdAt);
        return row;
    }

    private BiAlertRuleDO alert(String condition, LocalDateTime createdAt, boolean enabled) {
        BiAlertRuleDO row = new BiAlertRuleDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setAlertKey("success-rate-alert");
        row.setName("Success Rate Alert");
        row.setDatasetId(11L);
        row.setMetricKey("success_rate");
        row.setConditionJson(condition);
        row.setEnabled(enabled);
        row.setCreatedAt(createdAt);
        return row;
    }

    private BiDeliveryLogDO log(LocalDateTime createdAt) {
        BiDeliveryLogDO row = new BiDeliveryLogDO();
        row.setCreatedAt(createdAt);
        row.setStatus("TRIGGERED");
        return row;
    }
}
