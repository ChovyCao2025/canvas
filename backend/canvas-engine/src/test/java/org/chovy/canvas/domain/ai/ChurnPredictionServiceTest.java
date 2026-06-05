package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiPredictionRunDO;
import org.chovy.canvas.dal.dataobject.AiUserPredictionSnapshotDO;
import org.chovy.canvas.dal.mapper.AiPredictionRunMapper;
import org.chovy.canvas.dal.mapper.AiUserPredictionSnapshotMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChurnPredictionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-04T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void highIdleUserGetsHighChurnRisk() {
        ChurnPredictionService service = fixture().service;

        ChurnPredictionService.Prediction prediction = service.predict(snapshot("u1", 55, 1, 4, 0.5, 0, false));

        assertThat(prediction.probability()).isGreaterThanOrEqualTo(new BigDecimal("0.70000"));
        assertThat(prediction.band()).isEqualTo(ChurnPredictionService.BAND_HIGH);
    }

    @Test
    void recentlyActiveUserGetsLowChurnRisk() {
        ChurnPredictionService service = fixture().service;

        ChurnPredictionService.Prediction prediction = service.predict(snapshot("u1", 1, 30, 2, 0, 10, false));

        assertThat(prediction.probability()).isLessThan(new BigDecimal("0.40000"));
        assertThat(prediction.band()).isEqualTo(ChurnPredictionService.BAND_LOW);
    }

    @Test
    void sparseHistoryGetsDefaultMediumRiskAndLowConfidence() {
        ChurnPredictionService service = fixture().service;

        ChurnPredictionService.Prediction prediction = service.predict(snapshot("u1", 60, 1, 0, 0, 0, true));

        assertThat(prediction.probability()).isEqualByComparingTo("0.50000");
        assertThat(prediction.band()).isEqualTo(ChurnPredictionService.BAND_MEDIUM);
        assertThat(prediction.confidence()).isEqualByComparingTo("0.30000");
    }

    @Test
    void predictionStoresFeatureAndContributionJson() {
        ChurnPredictionService service = fixture().service;

        ChurnPredictionService.Prediction prediction = service.predict(snapshot("u1", 12, 5, 3, 0.25, 1, false));

        assertThat(prediction.features())
                .containsEntry("userId", "u1")
                .containsEntry("daysSinceLastEvent", 12)
                .containsEntry("eventCount30d", 5);
        assertThat(prediction.contributions())
                .containsKeys("base", "idleDays", "failures", "engagement", "goals");
    }

    @Test
    void runIsIdempotentForTenantModelVersionAndRunDate() {
        Fixture fixture = fixture();
        AiPredictionRunDO existing = new AiPredictionRunDO();
        existing.setId(10L);
        existing.setTenantId(7L);
        existing.setModelKey(ChurnPredictionService.MODEL_KEY);
        existing.setModelVersion("baseline_v1");
        existing.setRunDate(LocalDate.of(2026, 6, 4));
        existing.setStatus(AiPredictionRunDO.STATUS_SUCCESS);
        existing.setProcessedCount(3);
        existing.setSkippedCount(0);
        existing.setFailedCount(0);
        when(fixture.runMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        ChurnPredictionService.PredictionRunView result = fixture.service.recompute(
                7L,
                new ChurnPredictionService.RecomputeRequest(false, LocalDate.of(2026, 6, 4), 100));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.processedCount()).isEqualTo(3);
        verify(fixture.runMapper, never()).insert(any(AiPredictionRunDO.class));
    }

    @Test
    void recomputePersistsSnapshotsAndWritesProfileFields() {
        Fixture fixture = fixture();
        when(fixture.runMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            AiPredictionRunDO run = invocation.getArgument(0);
            run.setId(44L);
            return 1;
        }).when(fixture.runMapper).insert(any(AiPredictionRunDO.class));
        when(fixture.featureSnapshotService.candidateUserIds(10)).thenReturn(List.of("u1"));
        when(fixture.featureSnapshotService.extract("u1", LocalDate.of(2026, 6, 4)))
                .thenReturn(snapshot("u1", 50, 4, 2, 0.5, 0, false));
        when(fixture.smartTimingService.bestSendHour("u1", LocalDate.of(2026, 6, 4))).thenReturn(20);

        ChurnPredictionService.PredictionRunView result = fixture.service.recompute(
                7L,
                new ChurnPredictionService.RecomputeRequest(true, LocalDate.of(2026, 6, 4), 10));

        assertThat(result.status()).isEqualTo(AiPredictionRunDO.STATUS_SUCCESS);
        assertThat(result.processedCount()).isEqualTo(1);
        verify(fixture.snapshotMapper).insert(any(AiUserPredictionSnapshotDO.class));
        verify(fixture.profileWriter).write(any(), any(), any(), anyInt(), any());
    }

    @Test
    void recomputeCountsSkippedAgainstActualCandidateUsers() {
        Fixture fixture = fixture();
        when(fixture.runMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            AiPredictionRunDO run = invocation.getArgument(0);
            run.setId(45L);
            return 1;
        }).when(fixture.runMapper).insert(any(AiPredictionRunDO.class));
        when(fixture.featureSnapshotService.candidateUserIds(10)).thenReturn(List.of("u1"));
        when(fixture.featureSnapshotService.extract("u1", LocalDate.of(2026, 6, 4)))
                .thenReturn(snapshot("u1", 1, 30, 1, 0, 4, false));
        when(fixture.smartTimingService.bestSendHour("u1", LocalDate.of(2026, 6, 4))).thenReturn(9);

        ChurnPredictionService.PredictionRunView result = fixture.service.recompute(
                7L,
                new ChurnPredictionService.RecomputeRequest(true, LocalDate.of(2026, 6, 4), 10));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();
    }

    private static ChurnFeatureSnapshotService.FeatureSnapshot snapshot(String userId,
                                                                        int idleDays,
                                                                        int events,
                                                                        int sends,
                                                                        double failureRate,
                                                                        int goals,
                                                                        boolean sparse) {
        return new ChurnFeatureSnapshotService.FeatureSnapshot(
                userId,
                idleDays,
                events,
                sends,
                failureRate,
                goals,
                120,
                sparse);
    }

    private static Fixture fixture() {
        ChurnFeatureSnapshotService featureSnapshotService = mock(ChurnFeatureSnapshotService.class);
        SmartTimingService smartTimingService = mock(SmartTimingService.class);
        PredictionProfileWriter profileWriter = mock(PredictionProfileWriter.class);
        AiPredictionRunMapper runMapper = mock(AiPredictionRunMapper.class);
        AiUserPredictionSnapshotMapper snapshotMapper = mock(AiUserPredictionSnapshotMapper.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setModelVersion("baseline_v1");
        properties.setBatchSize(500);
        ChurnPredictionService service = new ChurnPredictionService(
                featureSnapshotService,
                smartTimingService,
                profileWriter,
                runMapper,
                snapshotMapper,
                properties,
                new ObjectMapper(),
                CLOCK);
        return new Fixture(featureSnapshotService, smartTimingService, profileWriter, runMapper, snapshotMapper, service);
    }

    private record Fixture(
            ChurnFeatureSnapshotService featureSnapshotService,
            SmartTimingService smartTimingService,
            PredictionProfileWriter profileWriter,
            AiPredictionRunMapper runMapper,
            AiUserPredictionSnapshotMapper snapshotMapper,
            ChurnPredictionService service) {
    }
}
