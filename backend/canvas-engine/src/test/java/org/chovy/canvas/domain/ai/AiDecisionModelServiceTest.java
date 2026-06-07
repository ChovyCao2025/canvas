package org.chovy.canvas.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiDecisionFeedbackDO;
import org.chovy.canvas.dal.dataobject.AiDecisionRunDO;
import org.chovy.canvas.dal.dataobject.AiPredictionRunDO;
import org.chovy.canvas.dal.dataobject.AiUserDecisionRecommendationDO;
import org.chovy.canvas.dal.dataobject.AiUserPredictionSnapshotDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.mapper.AiDecisionFeedbackMapper;
import org.chovy.canvas.dal.mapper.AiDecisionRunMapper;
import org.chovy.canvas.dal.mapper.AiUserDecisionRecommendationMapper;
import org.chovy.canvas.dal.mapper.AiUserPredictionSnapshotMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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

class AiDecisionModelServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDate RUN_DATE = LocalDate.of(2026, 6, 6);

    @Test
    void recomputeCreatesFourDecisionRecommendationsWithFeaturesAndExplanations() {
        Fixture fixture = fixture();
        doAnswer(invocation -> {
            invocation.<AiDecisionRunDO>getArgument(0).setId(501L);
            return 1;
        }).when(fixture.runMapper).insert(any(AiDecisionRunDO.class));
        when(fixture.profileMapper.selectOne(any())).thenReturn(profile("u1", "u1@example.com", "15551234567"));
        when(fixture.featureSnapshotService.extract("u1", RUN_DATE))
                .thenReturn(snapshot("u1", 45, 8, 4, 0.25, 2, false));
        when(fixture.smartTimingService.bestSendHour("u1", RUN_DATE)).thenReturn(20);
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of(churn("u1", "0.82000", "HIGH")));
        when(fixture.consentMapper.selectList(any())).thenReturn(List.of(consent("EMAIL"), consent("SMS")));
        ArgumentCaptor<AiUserDecisionRecommendationDO> captor =
                ArgumentCaptor.forClass(AiUserDecisionRecommendationDO.class);

        AiDecisionRunView view = fixture.service.recompute(7L, command(List.of("u1"), null), "operator-1");

        assertThat(view.status()).isEqualTo("SUCCESS");
        assertThat(view.processedCount()).isEqualTo(1);
        verify(fixture.recommendationMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        List<AiUserDecisionRecommendationDO> recommendations = captor.getAllValues();
        assertThat(recommendations)
                .extracting(AiUserDecisionRecommendationDO::getDecisionType)
                .containsExactlyInAnyOrder("LTV", "NEXT_BEST_ACTION", "NEXT_BEST_OFFER", "CHANNEL_AFFINITY");
        assertThat(recommendations)
                .allSatisfy(row -> {
                    assertThat(row.getTenantId()).isEqualTo(7L);
                    assertThat(row.getRunId()).isEqualTo(501L);
                    assertThat(row.getModelKey()).isEqualTo(AiDecisionModelService.MODEL_KEY);
                    assertThat(row.getModelVersion()).isEqualTo("decision_baseline_v1");
                    assertThat(row.getFeatureJson()).contains("churnProbability", "bestSendHour");
                    assertThat(row.getExplanationJson()).contains("contributions");
                    assertThat(row.getRecommendationRank()).isGreaterThan(0);
                    assertThat(row.getScore()).isBetween(new BigDecimal("0.00000"), new BigDecimal("1.00000"));
                });
        assertThat(recommendations)
                .filteredOn(row -> "NEXT_BEST_ACTION".equals(row.getDecisionType()))
                .singleElement()
                .satisfies(row -> assertThat(row.getActionKey()).isEqualTo("RETENTION_INTERVENTION"));
    }

    @Test
    void budgetCapMarksExpensiveOfferIneligibleWithoutDroppingAuditRow() {
        Fixture fixture = fixture();
        doAnswer(invocation -> {
            invocation.<AiDecisionRunDO>getArgument(0).setId(502L);
            return 1;
        }).when(fixture.runMapper).insert(any(AiDecisionRunDO.class));
        when(fixture.profileMapper.selectOne(any())).thenReturn(profile("u1", "u1@example.com", null));
        when(fixture.featureSnapshotService.extract("u1", RUN_DATE))
                .thenReturn(snapshot("u1", 50, 6, 3, 0.10, 1, false));
        when(fixture.smartTimingService.bestSendHour("u1", RUN_DATE)).thenReturn(19);
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of(churn("u1", "0.78000", "HIGH")));
        when(fixture.consentMapper.selectList(any())).thenReturn(List.of(consent("EMAIL")));
        ArgumentCaptor<AiUserDecisionRecommendationDO> captor =
                ArgumentCaptor.forClass(AiUserDecisionRecommendationDO.class);

        fixture.service.recompute(7L, command(List.of("u1"), new BigDecimal("10.0000")), "operator-1");

        verify(fixture.recommendationMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(row -> "NEXT_BEST_OFFER".equals(row.getDecisionType()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getEligibilityStatus()).isEqualTo("BUDGET_CONSTRAINED");
                    assertThat(row.getFallbackReason()).isEqualTo("BUDGET_CAP_EXCEEDED");
                    assertThat(row.getBudgetCost()).isGreaterThan(new BigDecimal("10.0000"));
                });
    }

    @Test
    void sparseHistoryWritesFallbackReasonAndLowConfidence() {
        Fixture fixture = fixture();
        doAnswer(invocation -> {
            invocation.<AiDecisionRunDO>getArgument(0).setId(503L);
            return 1;
        }).when(fixture.runMapper).insert(any(AiDecisionRunDO.class));
        when(fixture.profileMapper.selectOne(any())).thenReturn(profile("u1", null, null));
        when(fixture.featureSnapshotService.extract("u1", RUN_DATE))
                .thenReturn(snapshot("u1", 60, 1, 0, 0, 0, true));
        when(fixture.smartTimingService.bestSendHour("u1", RUN_DATE)).thenReturn(10);
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of());
        when(fixture.consentMapper.selectList(any())).thenReturn(List.of());
        ArgumentCaptor<AiUserDecisionRecommendationDO> captor =
                ArgumentCaptor.forClass(AiUserDecisionRecommendationDO.class);

        fixture.service.recompute(7L, command(List.of("u1"), null), "operator-1");

        verify(fixture.recommendationMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(row -> {
                    assertThat(row.getFallbackReason()).isEqualTo("SPARSE_HISTORY");
                    assertThat(row.getConfidence()).isLessThanOrEqualTo(new BigDecimal("0.40000"));
                });
    }

    @Test
    void recommendationQueriesAreTenantScopedAndBoundedToOneHundred() {
        Fixture fixture = fixture();
        List<AiUserDecisionRecommendationDO> rows = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            rows.add(recommendation(7L, "u" + i, "NEXT_BEST_ACTION", i + 1));
        }
        rows.add(recommendation(99L, "foreign", "NEXT_BEST_ACTION", 1));
        when(fixture.recommendationMapper.selectList(any())).thenReturn(rows);

        List<AiDecisionRecommendationView> result = fixture.service.recommendations(7L,
                new AiDecisionRecommendationQuery(null, "NEXT_BEST_ACTION", "ELIGIBLE", 500));

        assertThat(result).hasSize(100);
        assertThat(result).allSatisfy(row -> assertThat(row.tenantId()).isEqualTo(7L));
    }

    @Test
    void feedbackRejectsCrossTenantRecommendations() {
        Fixture fixture = fixture();
        when(fixture.recommendationMapper.selectById(100L))
                .thenReturn(recommendation(99L, "u1", "NEXT_BEST_ACTION", 1));

        assertThatThrownBy(() -> fixture.service.recordFeedback(7L, 100L,
                new AiDecisionFeedbackCommand("ACCEPTED", BigDecimal.ONE, Map.of()), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendation is not found");

        verify(fixture.feedbackMapper, never()).insert(any(AiDecisionFeedbackDO.class));
    }

    @Test
    void feedbackWritesActorOutcomeAndMetadata() {
        Fixture fixture = fixture();
        when(fixture.recommendationMapper.selectById(100L))
                .thenReturn(recommendation(7L, "u1", "NEXT_BEST_ACTION", 1));
        doAnswer(invocation -> {
            invocation.<AiDecisionFeedbackDO>getArgument(0).setId(77L);
            return 1;
        }).when(fixture.feedbackMapper).insert(any(AiDecisionFeedbackDO.class));
        ArgumentCaptor<AiDecisionFeedbackDO> captor = ArgumentCaptor.forClass(AiDecisionFeedbackDO.class);

        AiDecisionFeedbackView view = fixture.service.recordFeedback(7L, 100L,
                new AiDecisionFeedbackCommand("accepted", new BigDecimal("99.9000"), Map.of("canvasId", 123)),
                "operator-1");

        assertThat(view.id()).isEqualTo(77L);
        verify(fixture.feedbackMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getRecommendationId()).isEqualTo(100L);
        assertThat(captor.getValue().getFeedbackType()).isEqualTo("ACCEPTED");
        assertThat(captor.getValue().getOutcomeValue()).isEqualByComparingTo("99.9000");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
        assertThat(captor.getValue().getMetadataJson()).contains("canvasId");
    }

    private AiDecisionRecomputeCommand command(List<String> userIds, BigDecimal budgetCap) {
        return new AiDecisionRecomputeCommand(
                RUN_DATE,
                "DAILY_MARKETING",
                userIds,
                true,
                budgetCap,
                Map.of("source", "test"));
    }

    private ChurnFeatureSnapshotService.FeatureSnapshot snapshot(String userId,
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

    private CdpUserProfileDO profile(String userId, String email, String phone) {
        CdpUserProfileDO row = new CdpUserProfileDO();
        row.setTenantId(7L);
        row.setUserId(userId);
        row.setEmail(email);
        row.setPhone(phone);
        row.setStatus("ACTIVE");
        row.setFirstSeenAt(LocalDateTime.of(2025, 6, 6, 0, 0));
        row.setPropertiesJson("{\"member_tier\":\"GOLD\"}");
        return row;
    }

    private AiUserPredictionSnapshotDO churn(String userId, String probability, String band) {
        AiUserPredictionSnapshotDO row = new AiUserPredictionSnapshotDO();
        row.setTenantId(7L);
        row.setRunId(44L);
        row.setUserId(userId);
        row.setModelKey(ChurnPredictionService.MODEL_KEY);
        row.setModelVersion("baseline_v1");
        row.setChurnProbability(new BigDecimal(probability));
        row.setChurnRiskBand(band);
        row.setBestSendHour(20);
        row.setConfidence(new BigDecimal("0.80000"));
        return row;
    }

    private MarketingConsentDO consent(String channel) {
        MarketingConsentDO row = new MarketingConsentDO();
        row.setTenantId(7L);
        row.setUserId("u1");
        row.setChannel(channel);
        row.setConsentStatus(MarketingConsentDO.OPT_IN);
        return row;
    }

    private AiUserDecisionRecommendationDO recommendation(Long tenantId,
                                                          String userId,
                                                          String decisionType,
                                                          int rank) {
        AiUserDecisionRecommendationDO row = new AiUserDecisionRecommendationDO();
        row.setId((long) rank);
        row.setTenantId(tenantId);
        row.setRunId(501L);
        row.setUserId(userId);
        row.setModelKey(AiDecisionModelService.MODEL_KEY);
        row.setModelVersion("decision_baseline_v1");
        row.setDecisionScope("DAILY_MARKETING");
        row.setDecisionType(decisionType);
        row.setDecisionKey("RETENTION_INTERVENTION");
        row.setActionKey("RETENTION_INTERVENTION");
        row.setScore(new BigDecimal("0.80000"));
        row.setConfidence(new BigDecimal("0.70000"));
        row.setRecommendationRank(rank);
        row.setBudgetCost(new BigDecimal("0.0000"));
        row.setEligibilityStatus("ELIGIBLE");
        row.setFeatureJson("{}");
        row.setExplanationJson("{}");
        row.setCreatedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        return row;
    }

    private Fixture fixture() {
        ChurnFeatureSnapshotService featureSnapshotService = mock(ChurnFeatureSnapshotService.class);
        SmartTimingService smartTimingService = mock(SmartTimingService.class);
        AiDecisionRunMapper runMapper = mock(AiDecisionRunMapper.class);
        AiUserDecisionRecommendationMapper recommendationMapper =
                mock(AiUserDecisionRecommendationMapper.class);
        AiDecisionFeedbackMapper feedbackMapper = mock(AiDecisionFeedbackMapper.class);
        CdpUserProfileMapper profileMapper = mock(CdpUserProfileMapper.class);
        MarketingConsentMapper consentMapper = mock(MarketingConsentMapper.class);
        AiUserPredictionSnapshotMapper snapshotMapper = mock(AiUserPredictionSnapshotMapper.class);
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setModelVersion("decision_baseline_v1");
        properties.setBatchSize(500);
        AiDecisionModelService service = new AiDecisionModelService(
                featureSnapshotService,
                smartTimingService,
                runMapper,
                recommendationMapper,
                feedbackMapper,
                profileMapper,
                consentMapper,
                snapshotMapper,
                properties,
                new ObjectMapper(),
                CLOCK);
        return new Fixture(
                featureSnapshotService,
                smartTimingService,
                runMapper,
                recommendationMapper,
                feedbackMapper,
                profileMapper,
                consentMapper,
                snapshotMapper,
                service);
    }

    private record Fixture(
            ChurnFeatureSnapshotService featureSnapshotService,
            SmartTimingService smartTimingService,
            AiDecisionRunMapper runMapper,
            AiUserDecisionRecommendationMapper recommendationMapper,
            AiDecisionFeedbackMapper feedbackMapper,
            CdpUserProfileMapper profileMapper,
            MarketingConsentMapper consentMapper,
            AiUserPredictionSnapshotMapper snapshotMapper,
            AiDecisionModelService service) {
    }
}
