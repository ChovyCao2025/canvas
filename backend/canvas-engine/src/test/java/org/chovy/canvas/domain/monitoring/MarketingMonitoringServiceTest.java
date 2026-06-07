package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingCompetitorMentionDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingSentimentAnalysisDO;
import org.chovy.canvas.dal.mapper.MarketingCompetitorMentionMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingSentimentAnalysisMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

class MarketingMonitoringServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertsSourceWithNormalizedTypeAndIdempotentUpdate() {
        Fixture fixture = fixture();
        MarketingMonitorSourceDO existing = source(7L, 10L, "manual-social-listening", 1);
        when(fixture.sourceMapper.selectOne(any())).thenReturn(null).thenReturn(existing);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorSourceDO>getArgument(0).setId(10L);
            return 1;
        }).when(fixture.sourceMapper).insert(any(MarketingMonitorSourceDO.class));
        ArgumentCaptor<MarketingMonitorSourceDO> insertCaptor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);
        ArgumentCaptor<MarketingMonitorSourceDO> updateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);

        MarketingMonitorSourceView created = fixture.service.upsertSource(7L,
                new MarketingMonitorSourceCommand(
                        " Manual-Social-Listening ",
                        "manual",
                        "Manual Social Listening",
                        true,
                        Map.of("owner", "brand-team")),
                "operator-1");
        MarketingMonitorSourceView updated = fixture.service.upsertSource(7L,
                new MarketingMonitorSourceCommand(
                        "manual-social-listening",
                        "manual",
                        "Renamed Listening",
                        false,
                        Map.of("owner", "ops-team")),
                "operator-2");

        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.sourceKey()).isEqualTo("manual-social-listening");
        assertThat(created.sourceType()).isEqualTo("MANUAL");
        assertThat(updated.enabled()).isFalse();
        verify(fixture.sourceMapper).insert(insertCaptor.capture());
        verify(fixture.sourceMapper).updateById(updateCaptor.capture());
        assertThat(insertCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(insertCaptor.getValue().getSourceKey()).isEqualTo("manual-social-listening");
        assertThat(insertCaptor.getValue().getSourceType()).isEqualTo("MANUAL");
        assertThat(insertCaptor.getValue().getCreatedBy()).isEqualTo("operator-1");
        assertThat(updateCaptor.getValue().getDisplayName()).isEqualTo("Renamed Listening");
        assertThat(updateCaptor.getValue().getEnabled()).isZero();
    }

    @Test
    void ingestItemPersistsSentimentCompetitorMentionsAndNegativeAlerts() {
        Fixture fixture = fixture();
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L, 10L, "manual-social-listening", 1));
        when(fixture.itemMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorItemDO>getArgument(0).setId(100L);
            return 1;
        }).when(fixture.itemMapper).insert(any(MarketingMonitorItemDO.class));
        doAnswer(invocation -> {
            invocation.<MarketingSentimentAnalysisDO>getArgument(0).setId(200L);
            return 1;
        }).when(fixture.sentimentMapper).insert(any(MarketingSentimentAnalysisDO.class));
        doAnswer(invocation -> {
            MarketingCompetitorMentionDO row = invocation.getArgument(0);
            row.setId(row.getId() == null ? 300L : row.getId());
            return 1;
        }).when(fixture.competitorMapper).insert(any(MarketingCompetitorMentionDO.class));
        doAnswer(invocation -> {
            MarketingMonitorAlertDO row = invocation.getArgument(0);
            row.setId(row.getId() == null
                    ? ("COMPETITOR_NEGATIVE".equals(row.getAlertType()) ? 402L : 401L)
                    : row.getId());
            return 1;
        }).when(fixture.alertMapper).insert(any(MarketingMonitorAlertDO.class));
        ArgumentCaptor<MarketingMonitorItemDO> itemCaptor =
                ArgumentCaptor.forClass(MarketingMonitorItemDO.class);
        ArgumentCaptor<MarketingSentimentAnalysisDO> sentimentCaptor =
                ArgumentCaptor.forClass(MarketingSentimentAnalysisDO.class);
        ArgumentCaptor<MarketingCompetitorMentionDO> competitorCaptor =
                ArgumentCaptor.forClass(MarketingCompetitorMentionDO.class);
        ArgumentCaptor<MarketingMonitorAlertDO> alertCaptor =
                ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);

        MarketingMonitorIngestResult result = fixture.service.ingestItem(7L,
                new MarketingMonitorItemIngestCommand(
                        10L,
                        "post-1",
                        "https://example.com/post-1",
                        "author-1",
                        "our-brand",
                        "CompetitorX has bad and broken support, but our onboarding is good.",
                        "en",
                        LocalDateTime.of(2026, 6, 6, 10, 0),
                        Map.of("competitorx", List.of("CompetitorX", "CX")),
                        Map.of("provider", "manual")),
                "operator-1");

        assertThat(result.item().id()).isEqualTo(100L);
        assertThat(result.sentiment().sentimentLabel()).isEqualTo("NEGATIVE");
        assertThat(result.sentiment().sentimentScore()).isLessThan(BigDecimal.ZERO);
        assertThat(result.competitorMentions()).singleElement()
                .satisfies(mention -> {
                    assertThat(mention.competitorKey()).isEqualTo("competitorx");
                    assertThat(mention.sentimentLabel()).isEqualTo("NEGATIVE");
                    assertThat(mention.matchedTerms()).containsExactly("CompetitorX");
                });
        assertThat(result.alerts()).extracting(MarketingMonitorAlertView::alertType)
                .containsExactlyInAnyOrder("NEGATIVE_SENTIMENT", "COMPETITOR_NEGATIVE");
        verify(fixture.itemMapper).insert(itemCaptor.capture());
        verify(fixture.sentimentMapper).insert(sentimentCaptor.capture());
        verify(fixture.competitorMapper).insert(competitorCaptor.capture());
        verify(fixture.alertMapper, org.mockito.Mockito.times(2)).insert(alertCaptor.capture());
        assertThat(itemCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(itemCaptor.getValue().getSourceType()).isEqualTo("MANUAL");
        assertThat(sentimentCaptor.getValue().getKeywordHitsJson()).contains("bad", "broken", "good");
        assertThat(competitorCaptor.getValue().getMatchedTermsJson()).contains("CompetitorX");
        assertThat(alertCaptor.getAllValues()).extracting(MarketingMonitorAlertDO::getStatus)
                .containsOnly("OPEN");
    }

    @Test
    void ingestItemDispatchesCreatedAlertsWithoutFailingIngestion() {
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        Fixture fixture = fixture(fanoutService);
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L, 10L, "manual-social-listening", 1));
        when(fixture.itemMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MarketingMonitorItemDO>getArgument(0).setId(100L);
            return 1;
        }).when(fixture.itemMapper).insert(any(MarketingMonitorItemDO.class));
        doAnswer(invocation -> {
            invocation.<MarketingSentimentAnalysisDO>getArgument(0).setId(200L);
            return 1;
        }).when(fixture.sentimentMapper).insert(any(MarketingSentimentAnalysisDO.class));
        doAnswer(invocation -> {
            invocation.<MarketingCompetitorMentionDO>getArgument(0).setId(300L);
            return 1;
        }).when(fixture.competitorMapper).insert(any(MarketingCompetitorMentionDO.class));
        doAnswer(invocation -> {
            MarketingMonitorAlertDO row = invocation.getArgument(0);
            row.setId("COMPETITOR_NEGATIVE".equals(row.getAlertType()) ? 402L : 401L);
            return 1;
        }).when(fixture.alertMapper).insert(any(MarketingMonitorAlertDO.class));
        org.mockito.Mockito.doThrow(new IllegalStateException("delivery outage"))
                .when(fanoutService).dispatchAlert(any(), any(MarketingMonitorAlertDO.class), any());

        MarketingMonitorIngestResult result = fixture.service.ingestItem(7L,
                new MarketingMonitorItemIngestCommand(
                        10L,
                        "post-1",
                        "https://example.com/post-1",
                        "author-1",
                        "our-brand",
                        "CompetitorX has bad and broken support",
                        "en",
                        LocalDateTime.of(2026, 6, 6, 10, 0),
                        Map.of("competitorx", List.of("CompetitorX")),
                        Map.of("provider", "manual")),
                "operator-1");

        assertThat(result.alerts()).extracting(MarketingMonitorAlertView::id)
                .containsExactlyInAnyOrder(401L, 402L);
        verify(fanoutService, org.mockito.Mockito.times(2))
                .dispatchAlert(org.mockito.Mockito.eq(7L), any(MarketingMonitorAlertDO.class),
                        org.mockito.Mockito.eq("operator-1"));
    }

    @Test
    void ingestRejectsDisabledSourceWithoutPersistingItem() {
        Fixture fixture = fixture();
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L, 10L, "manual-social-listening", 0));

        assertThatThrownBy(() -> fixture.service.ingestItem(7L,
                new MarketingMonitorItemIngestCommand(
                        10L,
                        "post-1",
                        null,
                        null,
                        "our-brand",
                        "bad support",
                        null,
                        null,
                        Map.of(),
                        Map.of()),
                "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        verify(fixture.itemMapper, never()).insert(any(MarketingMonitorItemDO.class));
        verify(fixture.sentimentMapper, never()).insert(any(MarketingSentimentAnalysisDO.class));
    }

    @Test
    void itemAndAlertQueriesAreTenantScopedFilteredAndBounded() {
        Fixture fixture = fixture();
        List<MarketingMonitorItemDO> items = new ArrayList<>();
        List<MarketingSentimentAnalysisDO> sentiments = new ArrayList<>();
        List<MarketingCompetitorMentionDO> competitors = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            items.add(item(7L, (long) i, "post-" + i));
            sentiments.add(sentiment(7L, (long) i, "NEGATIVE"));
            competitors.add(competitor(7L, (long) i, "competitorx"));
        }
        items.add(item(99L, 999L, "foreign"));
        sentiments.add(sentiment(99L, 999L, "NEGATIVE"));
        competitors.add(competitor(99L, 999L, "competitorx"));
        when(fixture.itemMapper.selectList(any())).thenReturn(items);
        when(fixture.sentimentMapper.selectList(any())).thenReturn(sentiments);
        when(fixture.competitorMapper.selectList(any())).thenReturn(competitors);
        List<MarketingMonitorAlertDO> alerts = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            alerts.add(alert(7L, (long) i, "OPEN"));
        }
        alerts.add(alert(99L, 999L, "OPEN"));
        when(fixture.alertMapper.selectList(any())).thenReturn(alerts);

        List<MarketingMonitorItemView> itemViews = fixture.service.items(7L,
                new MarketingMonitorItemQuery("NEGATIVE", "competitorx", 500));
        List<MarketingMonitorAlertView> alertViews = fixture.service.alerts(7L,
                new MarketingMonitorAlertQuery("OPEN", 500));

        assertThat(itemViews).hasSize(100);
        assertThat(itemViews).allSatisfy(view -> {
            assertThat(view.tenantId()).isEqualTo(7L);
            assertThat(view.sentimentLabel()).isEqualTo("NEGATIVE");
            assertThat(view.competitorKeys()).contains("competitorx");
        });
        assertThat(alertViews).hasSize(100);
        assertThat(alertViews).allSatisfy(view -> {
            assertThat(view.tenantId()).isEqualTo(7L);
            assertThat(view.status()).isEqualTo("OPEN");
        });
    }

    @Test
    void alertResolutionRejectsCrossTenantAndPersistsResolver() {
        Fixture fixture = fixture();
        when(fixture.alertMapper.selectById(500L)).thenReturn(alert(99L, 500L, "OPEN"));

        assertThatThrownBy(() -> fixture.service.resolveAlert(7L, 500L, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alert is not found");

        verify(fixture.alertMapper, never()).updateById(any(MarketingMonitorAlertDO.class));

        when(fixture.alertMapper.selectById(501L)).thenReturn(alert(7L, 501L, "OPEN"));
        MarketingMonitorAlertView view = fixture.service.resolveAlert(7L, 501L, "operator-1");

        assertThat(view.status()).isEqualTo("RESOLVED");
        ArgumentCaptor<MarketingMonitorAlertDO> captor = ArgumentCaptor.forClass(MarketingMonitorAlertDO.class);
        verify(fixture.alertMapper).updateById(captor.capture());
        assertThat(captor.getValue().getResolvedBy()).isEqualTo("operator-1");
        assertThat(captor.getValue().getResolvedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 8, 0));
    }

    private Fixture fixture() {
        return fixture(null);
    }

    private Fixture fixture(MarketingMonitorAlertFanoutService fanoutService) {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorItemMapper itemMapper = mock(MarketingMonitorItemMapper.class);
        MarketingSentimentAnalysisMapper sentimentMapper = mock(MarketingSentimentAnalysisMapper.class);
        MarketingCompetitorMentionMapper competitorMapper = mock(MarketingCompetitorMentionMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        return new Fixture(
                sourceMapper,
                itemMapper,
                sentimentMapper,
                competitorMapper,
                alertMapper,
                new MarketingMonitoringService(
                        sourceMapper,
                        itemMapper,
                        sentimentMapper,
                        competitorMapper,
                        alertMapper,
                        new ObjectMapper(),
                        CLOCK,
                        fanoutService));
    }

    private MarketingMonitorSourceDO source(Long tenantId, Long id, String sourceKey, Integer enabled) {
        MarketingMonitorSourceDO row = new MarketingMonitorSourceDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSourceKey(sourceKey);
        row.setSourceType("MANUAL");
        row.setDisplayName("Manual Social Listening");
        row.setEnabled(enabled);
        row.setMetadataJson("{\"owner\":\"brand-team\"}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingMonitorItemDO item(Long tenantId, Long id, String externalItemId) {
        MarketingMonitorItemDO row = new MarketingMonitorItemDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSourceId(10L);
        row.setExternalItemId(externalItemId);
        row.setSourceType("MANUAL");
        row.setSourceUrl("https://example.com/" + externalItemId);
        row.setAuthorKey("author-1");
        row.setBrandKey("our-brand");
        row.setTextContent("bad competitorx support");
        row.setLanguage("en");
        row.setPublishedAt(now());
        row.setIngestedAt(now());
        row.setRawPayloadJson("{}");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private MarketingSentimentAnalysisDO sentiment(Long tenantId, Long itemId, String label) {
        MarketingSentimentAnalysisDO row = new MarketingSentimentAnalysisDO();
        row.setId(itemId + 2000);
        row.setTenantId(tenantId);
        row.setItemId(itemId);
        row.setSentimentLabel(label);
        row.setSentimentScore(new BigDecimal("-0.50000"));
        row.setConfidence(new BigDecimal("0.80000"));
        row.setModelKey(MarketingMonitoringService.SENTIMENT_MODEL_KEY);
        row.setModelVersion("lexicon_v1");
        row.setKeywordHitsJson("{\"negative\":[\"bad\"],\"positive\":[]}");
        row.setCreatedAt(now());
        return row;
    }

    private MarketingCompetitorMentionDO competitor(Long tenantId, Long itemId, String competitorKey) {
        MarketingCompetitorMentionDO row = new MarketingCompetitorMentionDO();
        row.setId(itemId + 3000);
        row.setTenantId(tenantId);
        row.setItemId(itemId);
        row.setCompetitorKey(competitorKey);
        row.setCompetitorName("CompetitorX");
        row.setMatchedTermsJson("[\"CompetitorX\"]");
        row.setSentimentLabel("NEGATIVE");
        row.setSentimentScore(new BigDecimal("-0.50000"));
        row.setCreatedAt(now());
        return row;
    }

    private MarketingMonitorAlertDO alert(Long tenantId, Long id, String status) {
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAlertType("NEGATIVE_SENTIMENT");
        row.setSeverity("HIGH");
        row.setStatus(status);
        row.setScopeKey("our-brand");
        row.setTitle("Negative sentiment detected");
        row.setReason("Detected negative sentiment");
        row.setItemCount(1);
        row.setWindowStart(now());
        row.setWindowEnd(now());
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }

    private record Fixture(MarketingMonitorSourceMapper sourceMapper,
                           MarketingMonitorItemMapper itemMapper,
                           MarketingSentimentAnalysisMapper sentimentMapper,
                           MarketingCompetitorMentionMapper competitorMapper,
                           MarketingMonitorAlertMapper alertMapper,
                           MarketingMonitoringService service) {
    }
}
