package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyEventDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyRuleDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorTrendSnapshotDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyRuleMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorTrendSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorAnomalyDetectionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 8, 0);

    @Test
    void upsertsRuleWithNormalizedMetricDirectionAndMetadata() {
        Fixture fixture = fixture();
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAnomalyRuleDO>getArgument(0).setId(60L);
            return 1;
        }).when(fixture.ruleMapper).insert(any(MarketingMonitorAnomalyRuleDO.class));

        MarketingMonitorAnomalyRuleView view = fixture.service.upsertRule(7L, ruleCommand(), "operator-1");

        assertThat(view.id()).isEqualTo(60L);
        assertThat(view.ruleKey()).isEqualTo("negative-spike");
        assertThat(view.metricKey()).isEqualTo("NEGATIVE_COUNT");
        assertThat(view.direction()).isEqualTo("SPIKE");
        assertThat(view.brandKey()).isEqualTo("our-brand");
        assertThat(view.metadata()).containsEntry("owner", "monitoring");
        ArgumentCaptor<MarketingMonitorAnomalyRuleDO> captor =
                ArgumentCaptor.forClass(MarketingMonitorAnomalyRuleDO.class);
        verify(fixture.ruleMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getSourceId()).isEqualTo(10L);
        assertThat(captor.getValue().getThresholdMultiplier()).isEqualByComparingTo("3.0000");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
        assertThat(captor.getValue().getMetadataJson()).contains("\"owner\":\"monitoring\"");
    }

    @Test
    void rejectsRuleWhenSourceBelongsToAnotherTenant() {
        Fixture fixture = fixture();
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(99L));

        assertThatThrownBy(() -> fixture.service.upsertRule(7L, ruleCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");

        verify(fixture.ruleMapper, never()).insert(any(MarketingMonitorAnomalyRuleDO.class));
        verify(fixture.ruleMapper, never()).updateById(any(MarketingMonitorAnomalyRuleDO.class));
    }

    @Test
    void detectsSpikeAgainstRollingMedianAndMadAndCreatesEventAndAlert() {
        Fixture fixture = fixture();
        when(fixture.ruleMapper.selectById(60L)).thenReturn(rule(7L, "SPIKE"));
        when(fixture.trendMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, NOW.minusDays(5), 10),
                snapshot(7L, NOW.minusDays(4), 11),
                snapshot(7L, NOW.minusDays(3), 12),
                snapshot(7L, NOW.minusDays(2), 10),
                snapshot(7L, NOW.minusDays(1), 11),
                snapshot(7L, NOW, 50),
                snapshot(99L, NOW.minusDays(1), 999)));
        doAnswer(invocation -> {
            invocation.<MarketingMonitorAnomalyEventDO>getArgument(0).setId(70L);
            return 1;
        }).when(fixture.eventMapper).insert(any(MarketingMonitorAnomalyEventDO.class));

        MarketingMonitorAnomalyDetectionView view = fixture.service.detect(7L,
                new MarketingMonitorAnomalyDetectionCommand(60L, NOW, NOW.plusDays(1)),
                "operator-1");

        assertThat(view.status()).isEqualTo("ANOMALY_DETECTED");
        assertThat(view.baselineBucketCount()).isEqualTo(5);
        assertThat(view.event()).isNotNull();
        assertThat(view.event().actualValue()).isEqualByComparingTo("50.000000");
        assertThat(view.event().baselineMedian()).isEqualByComparingTo("11.000000");
        assertThat(view.event().baselineMad()).isEqualByComparingTo("1.000000");
        assertThat(view.event().robustZScore()).isEqualByComparingTo("26.305500");
        assertThat(view.event().direction()).isEqualTo("SPIKE");
        assertThat(view.event().severity()).isEqualTo("CRITICAL");

        ArgumentCaptor<MarketingMonitorAnomalyEventDO> eventCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAnomalyEventDO.class);
        verify(fixture.eventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().getRuleId()).isEqualTo(60L);
        assertThat(eventCaptor.getValue().getEvidenceJson()).contains("baselineValues", "thresholdMultiplier");

        ArgumentCaptor<MarketingMonitorAlertDO> alertCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(fixture.alertMapper).insert(alertCaptor.capture());
        assertThat(alertCaptor.getValue().getAlertType()).isEqualTo("ANOMALY_DETECTED");
        assertThat(alertCaptor.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(alertCaptor.getValue().getScopeKey()).isEqualTo("our-brand");
    }

    @Test
    void skipsEventWhenTargetDoesNotExceedThreshold() {
        Fixture fixture = fixture();
        when(fixture.ruleMapper.selectById(60L)).thenReturn(rule(7L, "BOTH"));
        when(fixture.trendMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, NOW.minusDays(5), 10),
                snapshot(7L, NOW.minusDays(4), 11),
                snapshot(7L, NOW.minusDays(3), 12),
                snapshot(7L, NOW.minusDays(2), 10),
                snapshot(7L, NOW.minusDays(1), 11),
                snapshot(7L, NOW, 12)));

        MarketingMonitorAnomalyDetectionView view = fixture.service.detect(7L,
                new MarketingMonitorAnomalyDetectionCommand(60L, NOW, NOW.plusDays(1)),
                "operator-1");

        assertThat(view.status()).isEqualTo("NO_ANOMALY");
        assertThat(view.event()).isNull();
        verify(fixture.eventMapper, never()).insert(any(MarketingMonitorAnomalyEventDO.class));
        verify(fixture.alertMapper, never()).insert(any(MarketingMonitorAlertDO.class));
    }

    @Test
    void listsTenantScopedEventsAndResolvesEvent() {
        Fixture fixture = fixture();
        when(fixture.eventMapper.selectList(any())).thenReturn(List.of(
                event(7L, 70L, "OPEN"),
                event(99L, 99L, "OPEN")));
        MarketingMonitorAnomalyEventDO event = event(7L, 70L, "OPEN");
        when(fixture.eventMapper.selectById(70L)).thenReturn(event);

        List<MarketingMonitorAnomalyEventView> events = fixture.service.events(7L,
                new MarketingMonitorAnomalyEventQuery(60L, "open", 50));
        MarketingMonitorAnomalyEventView resolved = fixture.service.resolveEvent(7L, 70L, "operator-1");

        assertThat(events).singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo(70L));
        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.resolvedBy()).isEqualTo("operator-1");
        ArgumentCaptor<MarketingMonitorAnomalyEventDO> updateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAnomalyEventDO.class);
        verify(fixture.eventMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("RESOLVED");
        assertThat(updateCaptor.getValue().getResolvedBy()).isEqualTo("operator-1");
    }

    private Fixture fixture() {
        MarketingMonitorAnomalyRuleMapper ruleMapper = mock(MarketingMonitorAnomalyRuleMapper.class);
        MarketingMonitorAnomalyEventMapper eventMapper = mock(MarketingMonitorAnomalyEventMapper.class);
        MarketingMonitorTrendSnapshotMapper trendMapper = mock(MarketingMonitorTrendSnapshotMapper.class);
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorAnomalyDetectionService service = new MarketingMonitorAnomalyDetectionService(
                ruleMapper,
                eventMapper,
                trendMapper,
                sourceMapper,
                alertMapper,
                new ObjectMapper(),
                CLOCK);
        return new Fixture(service, ruleMapper, eventMapper, trendMapper, sourceMapper, alertMapper);
    }

    private MarketingMonitorAnomalyRuleCommand ruleCommand() {
        return new MarketingMonitorAnomalyRuleCommand(
                "negative-spike",
                "Negative mention spike",
                10L,
                "negative_count",
                "day",
                "our-brand",
                null,
                "spike",
                14,
                5,
                new BigDecimal("3.0000"),
                new BigDecimal("5.000000"),
                true,
                Map.of("owner", "monitoring"));
    }

    private MarketingMonitorSourceDO source(Long tenantId) {
        MarketingMonitorSourceDO source = new MarketingMonitorSourceDO();
        source.setId(10L);
        source.setTenantId(tenantId);
        source.setSourceKey("brandwatch");
        source.setSourceType("SOCIAL_LISTENING");
        source.setEnabled(1);
        return source;
    }

    private MarketingMonitorAnomalyRuleDO rule(Long tenantId, String direction) {
        MarketingMonitorAnomalyRuleDO row = new MarketingMonitorAnomalyRuleDO();
        row.setId(60L);
        row.setTenantId(tenantId);
        row.setRuleKey("negative-spike");
        row.setDisplayName("Negative mention spike");
        row.setSourceId(10L);
        row.setMetricKey("NEGATIVE_COUNT");
        row.setBucketGrain("DAY");
        row.setBrandKey("our-brand");
        row.setCompetitorKey("");
        row.setDirection(direction);
        row.setBaselineWindowBuckets(14);
        row.setMinBaselineBuckets(5);
        row.setThresholdMultiplier(new BigDecimal("3.0000"));
        row.setMinDelta(new BigDecimal("5.000000"));
        row.setEnabled(1);
        return row;
    }

    private MarketingMonitorTrendSnapshotDO snapshot(Long tenantId, LocalDateTime bucketStart, int negativeCount) {
        MarketingMonitorTrendSnapshotDO row = new MarketingMonitorTrendSnapshotDO();
        row.setId((long) negativeCount);
        row.setTenantId(tenantId);
        row.setSourceId(10L);
        row.setSourceKey("brandwatch");
        row.setBucketGrain("DAY");
        row.setBucketStart(bucketStart);
        row.setBucketEnd(bucketStart.plusDays(1));
        row.setBrandKey("our-brand");
        row.setCompetitorKey("");
        row.setMentionCount(100);
        row.setPositiveCount(10);
        row.setNeutralCount(80);
        row.setNegativeCount(negativeCount);
        row.setCompetitorCount(2);
        row.setAlertCount(1);
        row.setAvgSentimentScore(new BigDecimal("-0.10000"));
        return row;
    }

    private MarketingMonitorAnomalyEventDO event(Long tenantId, Long id, String status) {
        MarketingMonitorAnomalyEventDO row = new MarketingMonitorAnomalyEventDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setRuleId(60L);
        row.setRuleKey("negative-spike");
        row.setSourceId(10L);
        row.setSourceKey("brandwatch");
        row.setMetricKey("NEGATIVE_COUNT");
        row.setBucketGrain("DAY");
        row.setBucketStart(NOW);
        row.setBucketEnd(NOW.plusDays(1));
        row.setBrandKey("our-brand");
        row.setCompetitorKey("");
        row.setActualValue(new BigDecimal("50.000000"));
        row.setBaselineMedian(new BigDecimal("11.000000"));
        row.setBaselineMad(new BigDecimal("1.000000"));
        row.setRobustZScore(new BigDecimal("26.305500"));
        row.setDeltaValue(new BigDecimal("39.000000"));
        row.setDirection("SPIKE");
        row.setSeverity("CRITICAL");
        row.setStatus(status);
        row.setCreatedBy("operator-1");
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }

    private record Fixture(
            MarketingMonitorAnomalyDetectionService service,
            MarketingMonitorAnomalyRuleMapper ruleMapper,
            MarketingMonitorAnomalyEventMapper eventMapper,
            MarketingMonitorTrendSnapshotMapper trendMapper,
            MarketingMonitorSourceMapper sourceMapper,
            MarketingMonitorAlertMapper alertMapper) {
    }
}
