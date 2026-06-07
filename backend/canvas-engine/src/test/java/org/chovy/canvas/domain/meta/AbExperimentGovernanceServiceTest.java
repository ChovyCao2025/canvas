package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AbExperimentGovernanceDecisionDO;
import org.chovy.canvas.dal.dataobject.AbExperimentMetricDO;
import org.chovy.canvas.dal.dataobject.AbExperimentMetricSnapshotDO;
import org.chovy.canvas.dal.mapper.AbExperimentGovernanceDecisionMapper;
import org.chovy.canvas.dal.mapper.AbExperimentMetricMapper;
import org.chovy.canvas.dal.mapper.AbExperimentMetricSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbExperimentGovernanceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void evaluationBlocksWinnerWhenGuardrailBreaches() {
        AbExperimentMetricMapper metricMapper = mock(AbExperimentMetricMapper.class);
        AbExperimentMetricSnapshotMapper snapshotMapper = mock(AbExperimentMetricSnapshotMapper.class);
        AbExperimentGovernanceDecisionMapper decisionMapper = mock(AbExperimentGovernanceDecisionMapper.class);
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                metric("conversion_rate", "PRIMARY", "INCREASE", "0.04", null, 500),
                metric("unsubscribe_rate", "GUARDRAIL", "DECREASE", null, "0.0100", 500)));
        when(snapshotMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                snapshot("A", "conversion_rate", 20_000, "0.1000"),
                snapshot("B", "conversion_rate", 20_000, "0.1400"),
                snapshot("A", "unsubscribe_rate", 20_000, "0.0100"),
                snapshot("B", "unsubscribe_rate", 20_000, "0.0350")));
        AbExperimentGovernanceService service = new AbExperimentGovernanceService(
                metricMapper, snapshotMapper, decisionMapper, CLOCK);

        AbExperimentGovernanceService.Evaluation evaluation = service.evaluate(42L, "A");

        assertThat(evaluation.status()).isEqualTo("GUARDRAIL_BREACH");
        assertThat(evaluation.winnerVariantKey()).isNull();
        assertThat(evaluation.writebackStatus()).isEqualTo("BLOCKED");
        assertThat(evaluation.reasons()).anySatisfy(reason ->
                assertThat(reason).contains("unsubscribe_rate", "B", "breached"));
        ArgumentCaptor<AbExperimentGovernanceDecisionDO> decisionCaptor =
                ArgumentCaptor.forClass(AbExperimentGovernanceDecisionDO.class);
        verify(decisionMapper).insert(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue().getExperimentId()).isEqualTo(42L);
        assertThat(decisionCaptor.getValue().getStatus()).isEqualTo("GUARDRAIL_BREACH");
        assertThat(decisionCaptor.getValue().getWritebackStatus()).isEqualTo("BLOCKED");
    }

    @Test
    void evaluationMarksWinnerCandidateOnlyAfterSampleAndConfidencePass() {
        AbExperimentMetricMapper metricMapper = mock(AbExperimentMetricMapper.class);
        AbExperimentMetricSnapshotMapper snapshotMapper = mock(AbExperimentMetricSnapshotMapper.class);
        AbExperimentGovernanceDecisionMapper decisionMapper = mock(AbExperimentGovernanceDecisionMapper.class);
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                metric("conversion_rate", "PRIMARY", "INCREASE", "0.03", null, 500),
                metric("unsubscribe_rate", "GUARDRAIL", "DECREASE", null, "0.0100", 500)));
        when(snapshotMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                snapshot("A", "conversion_rate", 30_000, "0.1000"),
                snapshot("B", "conversion_rate", 30_000, "0.1400"),
                snapshot("A", "unsubscribe_rate", 30_000, "0.0100"),
                snapshot("B", "unsubscribe_rate", 30_000, "0.0120")));
        AbExperimentGovernanceService service = new AbExperimentGovernanceService(
                metricMapper, snapshotMapper, decisionMapper, CLOCK);

        AbExperimentGovernanceService.Evaluation evaluation = service.evaluate(42L, "A");

        assertThat(evaluation.status()).isEqualTo("WINNER_CANDIDATE");
        assertThat(evaluation.winnerVariantKey()).isEqualTo("B");
        assertThat(evaluation.primaryMetricKey()).isEqualTo("conversion_rate");
        assertThat(evaluation.confidence()).isGreaterThanOrEqualTo(new BigDecimal("0.950000"));
        assertThat(evaluation.writebackStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(evaluation.reasons()).anySatisfy(reason ->
                assertThat(reason).contains("manual review"));
    }

    @Test
    void evaluationRequiresMoreSampleBeforeCallingWinner() {
        AbExperimentMetricMapper metricMapper = mock(AbExperimentMetricMapper.class);
        AbExperimentMetricSnapshotMapper snapshotMapper = mock(AbExperimentMetricSnapshotMapper.class);
        AbExperimentGovernanceDecisionMapper decisionMapper = mock(AbExperimentGovernanceDecisionMapper.class);
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                metric("conversion_rate", "PRIMARY", "INCREASE", "0.03", null, 500)));
        when(snapshotMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                snapshot("A", "conversion_rate", 120, "0.1000"),
                snapshot("B", "conversion_rate", 120, "0.1800")));
        AbExperimentGovernanceService service = new AbExperimentGovernanceService(
                metricMapper, snapshotMapper, decisionMapper, CLOCK);

        AbExperimentGovernanceService.Evaluation evaluation = service.evaluate(42L, "A");

        assertThat(evaluation.status()).isEqualTo("INSUFFICIENT_SAMPLE");
        assertThat(evaluation.winnerVariantKey()).isNull();
        assertThat(evaluation.requiredSampleSizePerVariant()).isGreaterThan(120);
        assertThat(evaluation.writebackStatus()).isEqualTo("NOT_READY");
    }

    private AbExperimentMetricDO metric(String key,
                                        String role,
                                        String direction,
                                        String minimumDetectableEffect,
                                        String guardrailMaxRegression,
                                        Integer minimumSampleSize) {
        AbExperimentMetricDO metric = new AbExperimentMetricDO();
        metric.setId((long) key.hashCode());
        metric.setExperimentId(42L);
        metric.setMetricKey(key);
        metric.setDisplayName(key);
        metric.setMetricRole(role);
        metric.setDirection(direction);
        metric.setMinimumDetectableEffect(decimal(minimumDetectableEffect));
        metric.setGuardrailMaxRegression(decimal(guardrailMaxRegression));
        metric.setMinimumSampleSize(minimumSampleSize);
        metric.setEnabled(1);
        return metric;
    }

    private AbExperimentMetricSnapshotDO snapshot(String variantKey,
                                                  String metricKey,
                                                  long sampleSize,
                                                  String metricValue) {
        AbExperimentMetricSnapshotDO snapshot = new AbExperimentMetricSnapshotDO();
        snapshot.setExperimentId(42L);
        snapshot.setVariantKey(variantKey);
        snapshot.setMetricKey(metricKey);
        snapshot.setSampleSize(sampleSize);
        snapshot.setConversions(Math.round(sampleSize * Double.parseDouble(metricValue)));
        snapshot.setMetricValue(new BigDecimal(metricValue));
        snapshot.setObservedAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        return snapshot;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
