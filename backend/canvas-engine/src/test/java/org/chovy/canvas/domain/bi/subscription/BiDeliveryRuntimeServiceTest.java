package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.notification.NotificationCreateCommand;
import org.chovy.canvas.domain.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDeliveryRuntimeServiceTest {

    @Test
    void runSubscriptionCreatesChannelLogsAndNotification() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        NotificationService notificationService = mock(NotificationService.class);
        when(subscriptionMapper.selectById(31L)).thenReturn(subscription());
        BiDeliveryRuntimeService service = service(subscriptionMapper, alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, notificationService);

        BiDeliveryRunResult result = service.runSubscription(7L, 31L, "alice");

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        verify(notificationService).create(any(NotificationCreateCommand.class));
        assertThat(result.status()).isEqualTo("TRIGGERED");
        assertThat(logCaptor.getAllValues()).extracting(BiDeliveryLogDO::getChannel)
                .containsExactly("IN_APP", "EMAIL");
        assertThat(logCaptor.getAllValues()).extracting(BiDeliveryLogDO::getStatus)
                .containsExactly("DELIVERED", "PENDING_ADAPTER");
        assertThat(logCaptor.getAllValues().get(0).getNextRetryAt()).isNull();
        assertThat(logCaptor.getAllValues().get(1).getRetryCount()).isZero();
        assertThat(logCaptor.getAllValues().get(1).getMaxRetryCount()).isEqualTo(4);
        assertThat(logCaptor.getAllValues().get(1).getNextRetryAt()).isNotNull();
    }

    @Test
    void runAlertQueriesMetricAndCreatesTriggeredLogs() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        when(alertRuleMapper.selectById(41L)).thenReturn(alert());
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.82))),
                        1,
                        12,
                        "hash"));
        BiDeliveryRuntimeService service = service(subscriptionMapper, alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        assertThat(logCaptor.getAllValues()).extracting(BiDeliveryLogDO::getChannel)
                .containsExactly("EVALUATION", "LARK");
        assertThat(logCaptor.getAllValues().get(0).getMetricValue())
                .isEqualByComparingTo(BigDecimal.valueOf(0.82));
    }

    @Test
    void runAlertRecordsSkippedWhenConditionDoesNotMatch() {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        when(alertRuleMapper.selectById(41L)).thenReturn(alert());
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.96))),
                        1,
                        12,
                        "hash"));
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(logCaptor.getValue().getChannel()).isEqualTo("EVALUATION");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SKIPPED");
    }

    @Test
    void runAlertSuppressesMatchedDeliveryDuringSilenceWindow() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiAlertRuleDO row = alert();
        row.setConditionJson("""
                {
                  "operator":"LT",
                  "threshold":0.9,
                  "silence":{
                    "enabled":true,
                    "reason":"maintenance window",
                    "until":"2999-01-01T00:00:00"
                  }
                }
                """);
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.82))),
                        1,
                        12,
                        "hash"));
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(logCaptor.getValue().getChannel()).isEqualTo("EVALUATION");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SKIPPED");
        assertThat(logCaptor.getValue().getMessage()).isEqualTo("Alert is silenced");
        Map<?, ?> payload = objectMapper.readValue(logCaptor.getValue().getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> silence = (Map<?, ?>) extra.get("silence");
        assertThat(silence.get("silenced")).isEqualTo(true);
        assertThat(silence.get("reason")).isEqualTo("maintenance window");
    }

    @Test
    void runAlertTriggersAnomalyWhenCurrentValueDropsBelowBaseline() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiAlertRuleDO row = alert();
        row.setConditionJson("""
                {
                  "operator":"ANOMALY_DROP",
                  "baselineWindow":5,
                  "minSamples":3,
                  "sensitivity":2.0
                }
                """);
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(
                        historicalEvaluation(0.98),
                        historicalEvaluation(1.00),
                        historicalEvaluation(1.02)));
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        BiDeliveryLogDO evaluation = logCaptor.getAllValues().get(0);
        assertThat(evaluation.getStatus()).isEqualTo("TRIGGERED");
        assertThat(evaluation.getMessage()).isEqualTo("Alert anomaly detected");
        Map<?, ?> payload = objectMapper.readValue(evaluation.getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> anomaly = (Map<?, ?>) extra.get("anomaly");
        assertThat(anomaly.get("sampleCount")).isEqualTo(3);
        assertThat(anomaly.get("direction")).isEqualTo("DROP");
        assertThat(((Number) anomaly.get("baselineAverage")).doubleValue()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void runAlertTriggersMovingAverageAnomalyWhenRecentWindowDropsBelowOlderBaseline() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiAlertRuleDO row = alert();
        row.setConditionJson("""
                {
                  "operator":"ANOMALY_DROP",
                  "model":"MOVING_AVERAGE",
                  "baselineWindow":3,
                  "comparisonWindow":2,
                  "minSamples":3,
                  "minDeltaPercent":0.15
                }
                """);
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(
                        historicalEvaluation(0.72),
                        historicalEvaluation(0.99),
                        historicalEvaluation(1.00),
                        historicalEvaluation(1.01)));
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        BiDeliveryLogDO evaluation = logCaptor.getAllValues().get(0);
        Map<?, ?> payload = objectMapper.readValue(evaluation.getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> anomaly = (Map<?, ?>) extra.get("anomaly");
        assertThat(anomaly.get("model")).isEqualTo("MOVING_AVERAGE");
        assertThat(anomaly.get("comparisonWindow")).isEqualTo(2);
        assertThat(anomaly.get("comparisonSampleCount")).isEqualTo(2);
        assertThat(anomaly.get("baselineSampleCount")).isEqualTo(3);
        assertThat(((Number) anomaly.get("comparisonAverage")).doubleValue())
                .isCloseTo(0.71, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(((Number) anomaly.get("baselineAverage")).doubleValue())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(((Number) anomaly.get("deltaPercent")).doubleValue()).isGreaterThan(0.15);
    }

    @Test
    void runAlertTriggersWeekOverWeekAnomalyAgainstCalendarBaselineWindow() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiAlertRuleDO row = alert();
        row.setConditionJson("""
                {
                  "operator":"ANOMALY_DROP",
                  "model":"PERIOD_OVER_PERIOD",
                  "period":"WEEK_OVER_WEEK",
                  "baselineWindow":2,
                  "minSamples":2,
                  "calendarWindowHours":6,
                  "minDeltaPercent":0.20
                }
                """);
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        LocalDateTime now = LocalDateTime.now();
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(
                        historicalEvaluation(0.71, now.minusDays(1)),
                        historicalEvaluation(0.99, now.minusWeeks(1).minusHours(2)),
                        historicalEvaluation(1.01, now.minusWeeks(1).plusHours(1)),
                        historicalEvaluation(0.75, now.minusWeeks(2))));
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        BiDeliveryLogDO evaluation = logCaptor.getAllValues().get(0);
        Map<?, ?> payload = objectMapper.readValue(evaluation.getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> anomaly = (Map<?, ?>) extra.get("anomaly");
        assertThat(anomaly.get("model")).isEqualTo("PERIOD_OVER_PERIOD");
        assertThat(anomaly.get("period")).isEqualTo("WEEK_OVER_WEEK");
        assertThat(anomaly.get("calendarWindowHours")).isEqualTo(6);
        assertThat(anomaly.get("baselineSampleCount")).isEqualTo(2);
        assertThat(((Number) anomaly.get("baselineAverage")).doubleValue())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(((Number) anomaly.get("deltaPercent")).doubleValue()).isGreaterThan(0.20);
    }

    @Test
    void runAlertUsesNaturalMonthBoundaryForMonthOverMonthAnomalyBaseline() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BiAlertRuleDO row = alert();
        row.setConditionJson(objectMapper.writeValueAsString(Map.of(
                "operator", "ANOMALY_DROP",
                "model", "PERIOD_OVER_PERIOD",
                "period", "MONTH_OVER_MONTH",
                "baselineWindow", 2,
                "minSamples", 2,
                "naturalBoundary", true,
                "minDeltaPercent", 0.20)));
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        LocalDateTime previousMonthStart = LocalDate.now().withDayOfMonth(1).minusMonths(1).atStartOfDay();
        LocalDateTime previousMonthEnd = previousMonthStart.plusMonths(1).minusNanos(1);
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(
                        historicalEvaluation(0.55, previousMonthStart.minusNanos(1)),
                        historicalEvaluation(1.01, previousMonthEnd.minusDays(1)),
                        historicalEvaluation(0.99, previousMonthStart.plusDays(1)),
                        historicalEvaluation(0.60, previousMonthEnd.plusNanos(1))));
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        BiDeliveryLogDO evaluation = logCaptor.getAllValues().get(0);
        Map<?, ?> payload = objectMapper.readValue(evaluation.getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> anomaly = (Map<?, ?>) extra.get("anomaly");
        assertThat(anomaly.get("period")).isEqualTo("MONTH_OVER_MONTH");
        assertThat(anomaly.get("naturalBoundary")).isEqualTo(true);
        assertThat(anomaly.get("targetWindowStart")).isEqualTo(previousMonthStart.toString());
        assertThat(anomaly.get("targetWindowEnd")).isEqualTo(previousMonthEnd.toString());
        assertThat(anomaly.get("baselineSampleCount")).isEqualTo(2);
        assertThat(((Number) anomaly.get("baselineAverage")).doubleValue())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void runAlertUsesHolidayComparisonDateForPeriodOverPeriodAnomalyBaseline() throws Exception {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        LocalDate comparisonDate = LocalDate.now().minusYears(1).minusDays(10);
        BiAlertRuleDO row = alert();
        row.setConditionJson(objectMapper.writeValueAsString(Map.of(
                "operator", "ANOMALY_DROP",
                "model", "PERIOD_OVER_PERIOD",
                "period", "YEAR_OVER_YEAR",
                "baselineWindow", 2,
                "minSamples", 2,
                "holidayComparisonDate", comparisonDate.toString(),
                "holidayName", "spring-festival",
                "minDeltaPercent", 0.20)));
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(
                        historicalEvaluation(0.65, comparisonDate.minusDays(1).atTime(10, 0)),
                        historicalEvaluation(1.02, comparisonDate.atTime(15, 0)),
                        historicalEvaluation(0.98, comparisonDate.atTime(9, 0)),
                        historicalEvaluation(0.70, comparisonDate.plusDays(1).atTime(10, 0)),
                        historicalEvaluation(0.72, LocalDate.now().minusYears(1).atTime(10, 0))));
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        BiDeliveryLogDO evaluation = logCaptor.getAllValues().get(0);
        Map<?, ?> payload = objectMapper.readValue(evaluation.getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        Map<?, ?> anomaly = (Map<?, ?>) extra.get("anomaly");
        assertThat(anomaly.get("period")).isEqualTo("YEAR_OVER_YEAR");
        assertThat(anomaly.get("holidayAdjusted")).isEqualTo(true);
        assertThat(anomaly.get("holidayName")).isEqualTo("spring-festival");
        assertThat(anomaly.get("holidayComparisonDate")).isEqualTo(comparisonDate.toString());
        assertThat(anomaly.get("targetWindowStart")).isEqualTo(comparisonDate.atStartOfDay().toString());
        assertThat(anomaly.get("targetWindowEnd")).isEqualTo(comparisonDate.plusDays(1).atStartOfDay().minusNanos(1).toString());
        assertThat(anomaly.get("baselineSampleCount")).isEqualTo(2);
        assertThat(((Number) anomaly.get("baselineAverage")).doubleValue())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void runAlertSkipsAnomalyWhenBaselineSamplesAreInsufficient() {
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiAlertRuleDO row = alert();
        row.setConditionJson("""
                {
                  "operator":"ANOMALY_DROP",
                  "baselineWindow":5,
                  "minSamples":3,
                  "sensitivity":2.0
                }
                """);
        when(alertRuleMapper.selectById(41L)).thenReturn(row);
        when(datasetMapper.selectById(11L)).thenReturn(dataset());
        when(queryExecutionService.execute(any(BiQueryRequest.class), any()))
                .thenReturn(new BiQueryResult(
                        "canvas_daily_stats",
                        List.of(new BiQueryColumn("success_rate", "METRIC", "PERCENT")),
                        List.of(Map.of("success_rate", BigDecimal.valueOf(0.70))),
                        1,
                        12,
                        "hash"));
        when(deliveryLogMapper.selectList(any()))
                .thenReturn(List.of(historicalEvaluation(0.98), historicalEvaluation(1.00)));
        BiDeliveryRuntimeService service = service(mock(BiSubscriptionMapper.class), alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryRunResult result = service.runAlert(7L, "alice", "OPERATOR", 41L);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(logCaptor.getValue().getChannel()).isEqualTo("EVALUATION");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SKIPPED");
        assertThat(logCaptor.getValue().getMessage()).isEqualTo("Alert anomaly baseline is insufficient");
    }

    @Test
    void runSubscriptionUsesConfiguredWebhookAdapter() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAdapterService adapterService = mock(BiDeliveryAdapterService.class);
        BiSubscriptionDO row = subscription();
        row.setReceiverJson("{\"channels\":[\"WEBHOOK\"],\"webhookUrl\":\"https://example.test/bi\"}");
        when(subscriptionMapper.selectById(31L)).thenReturn(row);
        when(adapterService.deliver(any(BiDeliveryAdapterRequest.class)))
                .thenReturn(BiDeliveryAdapterResult.delivered("WEBHOOK webhook delivered: HTTP 200"));
        BiDeliveryRuntimeService service = service(subscriptionMapper, alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null, adapterService);

        BiDeliveryRunResult result = service.runSubscription(7L, 31L, "alice");

        ArgumentCaptor<BiDeliveryAdapterRequest> requestCaptor = ArgumentCaptor.forClass(BiDeliveryAdapterRequest.class);
        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(adapterService).deliver(requestCaptor.capture());
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.status()).isEqualTo("TRIGGERED");
        assertThat(requestCaptor.getValue().channel()).isEqualTo("WEBHOOK");
        assertThat(requestCaptor.getValue().receiver()).containsEntry("webhookUrl", "https://example.test/bi");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void runSubscriptionAddsAttachmentMetadataToDeliveryPayload() throws Exception {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        BiSubscriptionDO row = subscription();
        row.setReceiverJson("{\"channels\":[\"EMAIL\"],\"users\":[\"alice\"]}");
        row.setDeliveryJson("{\"content\":\"SNAPSHOT_LINK\",\"attachment\":\"PDF\"}");
        when(subscriptionMapper.selectById(31L)).thenReturn(row);
        when(attachmentService.createSubscriptionAttachments(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new BiDeliveryAttachmentView(
                        71L,
                        7L,
                        5L,
                        "SUBSCRIPTION",
                        31L,
                        "canvas-daily",
                        null,
                        "DASHBOARD",
                        21L,
                        "canvas-daily-pdf",
                        "PDF",
                        "canvas-daily.pdf",
                        "application/pdf",
                        "/canvas/bi/delivery-attachments/71/download",
                        null,
                        null,
                        128L,
                        7,
                        null,
                        0,
                        null,
                        "COMPLETED",
                        null,
                        "alice",
                        null,
                        null)));
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                null,
                null,
                attachmentService,
                objectMapper);

        service.runSubscription(7L, 31L, "alice");

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        Map<?, ?> payload = objectMapper.readValue(logCaptor.getValue().getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        List<?> attachments = (List<?>) extra.get("attachments");
        assertThat(attachments).singleElement().satisfies(value -> {
            Map<?, ?> attachment = (Map<?, ?>) value;
            assertThat(attachment.get("attachmentType")).isEqualTo("PDF");
            assertThat(attachment.get("fileUrl")).isEqualTo("/canvas/bi/delivery-attachments/71/download");
        });
    }

    @Test
    void runSubscriptionPayloadUsesBigScreenWorkbenchModeUrl() throws Exception {
        assertSubscriptionUrl("BIG_SCREEN", 51L,
                "/bi?resourceType=BIG_SCREEN&resourceId=51&mode=big-screen");
    }

    @Test
    void runSubscriptionPayloadUsesSpreadsheetWorkbenchModeUrl() throws Exception {
        assertSubscriptionUrl("SPREADSHEET", 61L,
                "/bi?resourceType=SPREADSHEET&resourceId=61&mode=spreadsheet");
    }

    @Test
    void runSubscriptionPassesGeneratedAttachmentsToEmailAdapter() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        BiDeliveryAdapterService adapterService = mock(BiDeliveryAdapterService.class);
        BiSubscriptionDO row = subscription();
        row.setReceiverJson("{\"channels\":[\"EMAIL\"],\"emails\":[\"alice@example.test\"]}");
        row.setDeliveryJson("{\"content\":\"SNAPSHOT_LINK\",\"attachment\":\"CSV\"}");
        when(subscriptionMapper.selectById(31L)).thenReturn(row);
        BiDeliveryAttachmentView attachment = new BiDeliveryAttachmentView(
                72L,
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                null,
                "DASHBOARD",
                21L,
                "canvas-daily-csv",
                "CSV",
                "canvas-daily.csv",
                "text/csv; charset=UTF-8",
                "/canvas/bi/delivery-attachments/72/download",
                null,
                null,
                64L,
                7,
                null,
                0,
                null,
                "COMPLETED",
                null,
                "alice",
                null,
                null);
        when(attachmentService.createSubscriptionAttachments(any(), any(), any(), any(), any()))
                .thenReturn(List.of(attachment));
        when(attachmentService.download(7L, 72L))
                .thenReturn(new BiDeliveryAttachmentDownload(
                        "canvas-daily.csv",
                        "text/csv; charset=UTF-8",
                        "key,value\njobKey,canvas-daily\n".getBytes(StandardCharsets.UTF_8)));
        when(adapterService.deliver(any(BiDeliveryAdapterRequest.class)))
                .thenReturn(BiDeliveryAdapterResult.delivered("Email delivered to 1 recipient(s) with 1 attachment(s)"));
        BiDeliveryRuntimeService service = new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                null,
                adapterService,
                attachmentService,
                new ObjectMapper());

        service.runSubscription(7L, 31L, "alice");

        ArgumentCaptor<BiDeliveryAdapterRequest> requestCaptor = ArgumentCaptor.forClass(BiDeliveryAdapterRequest.class);
        verify(adapterService).deliver(requestCaptor.capture());
        assertThat(requestCaptor.getValue().attachments()).singleElement().satisfies(emailAttachment -> {
            assertThat(emailAttachment.fileName()).isEqualTo("canvas-daily.csv");
            assertThat(new String(emailAttachment.bytes(), StandardCharsets.UTF_8)).contains("jobKey,canvas-daily");
        });
    }

    @Test
    void retryPendingDeliveriesReplaysRetryableLogsThroughAdapter() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAdapterService adapterService = mock(BiDeliveryAdapterService.class);
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(retryableWebhookLog()));
        when(adapterService.deliver(any(BiDeliveryAdapterRequest.class)))
                .thenReturn(BiDeliveryAdapterResult.delivered("WEBHOOK webhook delivered: HTTP 200"));
        BiDeliveryRuntimeService service = service(subscriptionMapper, alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null, adapterService);

        BiDeliveryRetryResult result = service.retryPendingDeliveries(7L, "alice", 10);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.delivered()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(logCaptor.getValue().getTriggeredBy()).isEqualTo("alice");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("DELIVERED");
        assertThat(logCaptor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(logCaptor.getValue().getLastRetryAt()).isNotNull();
        assertThat(logCaptor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void retryPendingDeliveriesMarksExhaustedWhenBackoffLimitReached() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAdapterService adapterService = mock(BiDeliveryAdapterService.class);
        BiDeliveryLogDO retryable = retryableWebhookLog();
        retryable.setRetryCount(1);
        retryable.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(retryable));
        when(adapterService.deliver(any(BiDeliveryAdapterRequest.class)))
                .thenReturn(BiDeliveryAdapterResult.failed("webhook failed", "HTTP 500"));
        BiDeliveryRuntimeService service = new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                null,
                adapterService,
                null,
                new ObjectMapper(),
                2,
                30,
                2.0,
                1440);

        BiDeliveryRetryResult result = service.retryPendingDeliveries(7L, "alice", 10);

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        assertThat(result.failed()).isEqualTo(1);
        assertThat(logCaptor.getValue().getRetryCount()).isEqualTo(2);
        assertThat(logCaptor.getValue().getMaxRetryCount()).isEqualTo(2);
        assertThat(logCaptor.getValue().getRetryExhaustedAt()).isNotNull();
        assertThat(logCaptor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void retryPendingEmailDeliveryReplaysHistoricalAttachmentsFromPayload() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryAdapterService adapterService = mock(BiDeliveryAdapterService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(retryableEmailLogWithAttachment()));
        when(attachmentService.download(7L, 72L))
                .thenReturn(new BiDeliveryAttachmentDownload(
                        "canvas-daily.csv",
                        "text/csv; charset=UTF-8",
                        "key,value\njobKey,canvas-daily\n".getBytes(StandardCharsets.UTF_8)));
        when(adapterService.deliver(any(BiDeliveryAdapterRequest.class)))
                .thenReturn(BiDeliveryAdapterResult.delivered("Email delivered to 1 recipient(s) with 1 attachment(s)"));
        BiDeliveryRuntimeService service = new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                null,
                adapterService,
                attachmentService,
                new ObjectMapper());

        BiDeliveryRetryResult result = service.retryPendingDeliveries(7L, "alice", 10);

        ArgumentCaptor<BiDeliveryAdapterRequest> requestCaptor = ArgumentCaptor.forClass(BiDeliveryAdapterRequest.class);
        verify(adapterService).deliver(requestCaptor.capture());
        assertThat(result.delivered()).isEqualTo(1);
        assertThat(requestCaptor.getValue().attachments()).singleElement().satisfies(emailAttachment -> {
            assertThat(emailAttachment.fileName()).isEqualTo("canvas-daily.csv");
            assertThat(new String(emailAttachment.bytes(), StandardCharsets.UTF_8)).contains("jobKey,canvas-daily");
        });
    }

    @Test
    void auditDeliveriesSummarizesFilteredDeliveryLogWindow() {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiDeliveryLogDO failed = retryableWebhookLog();
        failed.setStatus("FAILED");
        failed.setRetryCount(2);
        failed.setMaxRetryCount(4);
        failed.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
        BiDeliveryLogDO exhausted = retryableWebhookLog();
        exhausted.setId(93L);
        exhausted.setStatus("FAILED");
        exhausted.setRetryCount(4);
        exhausted.setMaxRetryCount(4);
        exhausted.setRetryExhaustedAt(LocalDateTime.now());
        BiDeliveryLogDO delivered = retryableWebhookLog();
        delivered.setId(94L);
        delivered.setStatus("DELIVERED");
        delivered.setRetryCount(1);
        when(deliveryLogMapper.selectList(any())).thenReturn(List.of(failed, exhausted, delivered));
        BiDeliveryRuntimeService service = service(subscriptionMapper, alertRuleMapper, datasetMapper,
                deliveryLogMapper, queryExecutionService, null);

        BiDeliveryAuditSummary summary = service.auditDeliveries(7L, "SUBSCRIPTION", "FAILED", "WEBHOOK", 31L, 25);

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.delivered()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(2);
        assertThat(summary.retryable()).isEqualTo(1);
        assertThat(summary.retryExhausted()).isEqualTo(1);
        assertThat(summary.logs()).hasSize(3);
    }

    private BiDeliveryRuntimeService service(BiSubscriptionMapper subscriptionMapper,
                                             BiAlertRuleMapper alertRuleMapper,
                                             BiDatasetMapper datasetMapper,
                                             BiDeliveryLogMapper deliveryLogMapper,
                                             BiQueryExecutionService queryExecutionService,
                                             NotificationService notificationService) {
        return service(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationService,
                null);
    }

    private BiDeliveryRuntimeService service(BiSubscriptionMapper subscriptionMapper,
                                             BiAlertRuleMapper alertRuleMapper,
                                             BiDatasetMapper datasetMapper,
                                             BiDeliveryLogMapper deliveryLogMapper,
                                             BiQueryExecutionService queryExecutionService,
                                             NotificationService notificationService,
                                             BiDeliveryAdapterService adapterService) {
        return new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationService,
                adapterService,
                new ObjectMapper());
    }

    private void assertSubscriptionUrl(String resourceType, Long resourceId, String expectedUrl) throws Exception {
        BiSubscriptionMapper subscriptionMapper = mock(BiSubscriptionMapper.class);
        BiAlertRuleMapper alertRuleMapper = mock(BiAlertRuleMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDeliveryLogMapper deliveryLogMapper = mock(BiDeliveryLogMapper.class);
        BiQueryExecutionService queryExecutionService = mock(BiQueryExecutionService.class);
        BiSubscriptionDO row = subscription();
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setReceiverJson("{\"channels\":[\"EMAIL\"],\"users\":[\"alice\"]}");
        when(subscriptionMapper.selectById(31L)).thenReturn(row);
        ObjectMapper objectMapper = new ObjectMapper();
        BiDeliveryRuntimeService service = new BiDeliveryRuntimeService(
                subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                null,
                objectMapper);

        service.runSubscription(7L, 31L, "alice");

        ArgumentCaptor<BiDeliveryLogDO> logCaptor = ArgumentCaptor.forClass(BiDeliveryLogDO.class);
        verify(deliveryLogMapper).insert(logCaptor.capture());
        Map<?, ?> payload = objectMapper.readValue(logCaptor.getValue().getPayloadJson(), Map.class);
        Map<?, ?> extra = (Map<?, ?>) payload.get("extra");
        assertThat(payload.get("url")).isEqualTo(expectedUrl);
        assertThat(extra.get("url")).isEqualTo(expectedUrl);
    }

    private BiSubscriptionDO subscription() {
        BiSubscriptionDO row = new BiSubscriptionDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSubscriptionKey("canvas-daily");
        row.setName("Canvas Daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setScheduleJson("{\"frequency\":\"DAILY\",\"time\":\"09:00\"}");
        row.setReceiverJson("{\"channels\":[\"IN_APP\",\"EMAIL\"],\"users\":[\"alice\"]}");
        row.setDeliveryJson("{\"content\":\"SNAPSHOT_LINK\"}");
        row.setEnabled(true);
        return row;
    }

    private BiAlertRuleDO alert() {
        BiAlertRuleDO row = new BiAlertRuleDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setAlertKey("success-rate-alert");
        row.setName("Success Rate Alert");
        row.setDatasetId(11L);
        row.setMetricKey("success_rate");
        row.setConditionJson("{\"operator\":\"LT\",\"threshold\":0.9}");
        row.setReceiverJson("{\"channels\":[\"LARK\"],\"users\":[\"alice\"]}");
        row.setEnabled(true);
        return row;
    }

    private BiDatasetDO dataset() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setDatasetKey("canvas_daily_stats");
        return row;
    }

    private BiDeliveryLogDO historicalEvaluation(double value) {
        return historicalEvaluation(value, LocalDateTime.now().minusDays(1));
    }

    private BiDeliveryLogDO historicalEvaluation(double value, LocalDateTime createdAt) {
        BiDeliveryLogDO row = new BiDeliveryLogDO();
        row.setId(80L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setJobType("ALERT");
        row.setJobId(41L);
        row.setJobKey("success-rate-alert");
        row.setResourceType("DATASET");
        row.setResourceId(11L);
        row.setChannel("EVALUATION");
        row.setMetricValue(BigDecimal.valueOf(value));
        row.setStatus("SKIPPED");
        row.setTriggeredBy("scheduler");
        row.setCreatedAt(createdAt);
        return row;
    }

    private BiDeliveryLogDO retryableWebhookLog() {
        BiDeliveryLogDO row = new BiDeliveryLogDO();
        row.setId(91L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setJobType("SUBSCRIPTION");
        row.setJobId(31L);
        row.setJobKey("canvas-daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setChannel("WEBHOOK");
        row.setReceiverJson("{\"webhookUrl\":\"https://example.test/bi\"}");
        row.setPayloadJson("{\"title\":\"Canvas Daily\",\"message\":\"BI subscription delivery is ready\"}");
        row.setStatus("FAILED");
        row.setTriggeredBy("scheduler");
        return row;
    }

    private BiDeliveryLogDO retryableEmailLogWithAttachment() {
        BiDeliveryLogDO row = new BiDeliveryLogDO();
        row.setId(92L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setJobType("SUBSCRIPTION");
        row.setJobId(31L);
        row.setJobKey("canvas-daily");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setChannel("EMAIL");
        row.setReceiverJson("{\"emails\":[\"alice@example.test\"]}");
        row.setPayloadJson("""
                {
                  "title":"Canvas Daily",
                  "message":"BI subscription delivery is ready",
                  "extra":{
                    "attachments":[
                      {
                        "id":72,
                        "attachmentType":"CSV",
                        "fileName":"canvas-daily.csv",
                        "fileUrl":"/canvas/bi/delivery-attachments/72/download",
                        "status":"COMPLETED"
                      }
                    ]
                  }
                }
                """);
        row.setStatus("FAILED");
        row.setTriggeredBy("scheduler");
        return row;
    }
}
