package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingCompetitorMentionDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorItemDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorPollRunDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorTrendSnapshotDO;
import org.chovy.canvas.dal.dataobject.MarketingSentimentAnalysisDO;
import org.chovy.canvas.dal.mapper.MarketingCompetitorMentionMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorItemMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorPollRunMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorTrendSnapshotMapper;
import org.chovy.canvas.dal.mapper.MarketingSentimentAnalysisMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitorPollingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 8, 0);

    @Test
    void configurePollingUpdatesSourcePollingState() {
        Fixture fixture = fixture(new FakePollClient());
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L, 10L, "brandwatch", 1, 0));

        MarketingMonitorSourcePollingView view = fixture.service.configurePolling(7L, 10L,
                new MarketingMonitorSourcePollingCommand(true, 15, "cursor-1", NOW.plusMinutes(15)),
                "operator-1");

        assertThat(view.sourceId()).isEqualTo(10L);
        assertThat(view.pollEnabled()).isTrue();
        assertThat(view.pollIntervalMinutes()).isEqualTo(15);
        assertThat(view.pollCursor()).isEqualTo("cursor-1");

        ArgumentCaptor<MarketingMonitorSourceDO> updateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);
        verify(fixture.sourceMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(updateCaptor.getValue().getPollEnabled()).isEqualTo(1);
        assertThat(updateCaptor.getValue().getPollIntervalMinutes()).isEqualTo(15);
        assertThat(updateCaptor.getValue().getPollCursor()).isEqualTo("cursor-1");
        assertThat(updateCaptor.getValue().getNextPollAt()).isEqualTo(NOW.plusMinutes(15));
    }

    @Test
    void pollSourceIngestsNewItemsCountsDuplicatesAndAdvancesCursor() {
        FakePollClient client = new FakePollClient(new MarketingMonitorPollResponse(
                List.of(
                        pollItem("post-duplicate", "duplicate mention", 1),
                        pollItem("post-new", "CompetitorX has bad support", 2)),
                "cursor-after",
                Map.of("providerRequestId", "req-1")));
        Fixture fixture = fixture(client);
        MarketingMonitorSourceDO source = source(7L, 10L, "brandwatch", 1, 1);
        source.setPollCursor("cursor-before");
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source);
        when(fixture.itemMapper.selectOne(any(Wrapper.class)))
                .thenReturn(item(7L, 100L, "post-duplicate"))
                .thenReturn(null);
        when(fixture.runMapper.insert(any(MarketingMonitorPollRunDO.class))).thenAnswer(invocation -> {
            invocation.<MarketingMonitorPollRunDO>getArgument(0).setId(900L);
            return 1;
        });
        when(fixture.monitoringService.ingestItem(eq(7L), any(), eq("operator-1")))
                .thenReturn(ingestResult("post-new", 1));

        MarketingMonitorPollRunView view = fixture.service.pollSource(7L, 10L,
                new MarketingMonitorPollCommand(NOW.minusHours(1), NOW, null, 50, false),
                "operator-1");

        assertThat(view.id()).isEqualTo(900L);
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.itemCount()).isEqualTo(2);
        assertThat(view.insertedCount()).isEqualTo(1);
        assertThat(view.duplicateCount()).isEqualTo(1);
        assertThat(view.alertCount()).isEqualTo(1);
        assertThat(view.cursorBefore()).isEqualTo("cursor-before");
        assertThat(view.cursorAfter()).isEqualTo("cursor-after");
        assertThat(client.request().cursor()).isEqualTo("cursor-before");
        assertThat(client.request().maxItems()).isEqualTo(50);

        ArgumentCaptor<MarketingMonitorItemIngestCommand> ingestCaptor =
                ArgumentCaptor.forClass(MarketingMonitorItemIngestCommand.class);
        verify(fixture.monitoringService).ingestItem(eq(7L), ingestCaptor.capture(), eq("operator-1"));
        assertThat(ingestCaptor.getValue().externalItemId()).isEqualTo("post-new");
        assertThat(ingestCaptor.getValue().competitors()).containsKey("competitorx");

        ArgumentCaptor<MarketingMonitorPollRunDO> runUpdateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorPollRunDO.class);
        verify(fixture.runMapper).updateById(runUpdateCaptor.capture());
        assertThat(runUpdateCaptor.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(runUpdateCaptor.getValue().getDuplicateCount()).isEqualTo(1);
        assertThat(runUpdateCaptor.getValue().getCursorAfter()).isEqualTo("cursor-after");

        ArgumentCaptor<MarketingMonitorSourceDO> sourceUpdateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);
        verify(fixture.sourceMapper).updateById(sourceUpdateCaptor.capture());
        assertThat(sourceUpdateCaptor.getValue().getPollCursor()).isEqualTo("cursor-after");
        assertThat(sourceUpdateCaptor.getValue().getLastPollStatus()).isEqualTo("COMPLETED");
        assertThat(sourceUpdateCaptor.getValue().getLastPolledAt()).isEqualTo(NOW);
    }

    @Test
    void pollSourceRecordsFailureWithoutAdvancingCursor() {
        FakePollClient client = new FakePollClient(new IllegalStateException("provider unavailable"));
        Fixture fixture = fixture(client);
        MarketingMonitorSourceDO source = source(7L, 10L, "brandwatch", 1, 1);
        source.setPollCursor("cursor-before");
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source);
        when(fixture.runMapper.insert(any(MarketingMonitorPollRunDO.class))).thenAnswer(invocation -> {
            invocation.<MarketingMonitorPollRunDO>getArgument(0).setId(901L);
            return 1;
        });

        MarketingMonitorPollRunView view = fixture.service.pollSource(7L, 10L,
                new MarketingMonitorPollCommand(NOW.minusHours(1), NOW, null, 50, false),
                "operator-1");

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.errorMessage()).contains("provider unavailable");
        ArgumentCaptor<MarketingMonitorPollRunDO> runUpdateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorPollRunDO.class);
        verify(fixture.runMapper).updateById(runUpdateCaptor.capture());
        assertThat(runUpdateCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runUpdateCaptor.getValue().getCursorAfter()).isNull();
        ArgumentCaptor<MarketingMonitorSourceDO> sourceUpdateCaptor =
                ArgumentCaptor.forClass(MarketingMonitorSourceDO.class);
        verify(fixture.sourceMapper).updateById(sourceUpdateCaptor.capture());
        assertThat(sourceUpdateCaptor.getValue().getPollCursor()).isNull();
        assertThat(sourceUpdateCaptor.getValue().getLastPollStatus()).isEqualTo("FAILED");
        verify(fixture.monitoringService, never()).ingestItem(any(), any(), any());
    }

    @Test
    void buildTrendSnapshotAggregatesTenantScopedMentionsSentimentCompetitorsAndAlerts() {
        Fixture fixture = fixture(new FakePollClient());
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(7L, 10L, "brandwatch", 1, 1));
        when(fixture.itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                item(7L, 100L, "post-1"),
                item(7L, 101L, "post-2"),
                item(7L, 102L, "post-3"),
                item(99L, 999L, "foreign")));
        when(fixture.sentimentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                sentiment(7L, 100L, "NEGATIVE", "-1.00000"),
                sentiment(7L, 101L, "POSITIVE", "1.00000"),
                sentiment(7L, 102L, "NEUTRAL", "0.00000"),
                sentiment(99L, 999L, "NEGATIVE", "-1.00000")));
        when(fixture.competitorMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                competitor(7L, 100L, "competitorx"),
                competitor(99L, 999L, "competitorx")));
        when(fixture.alertMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                alert(7L, 401L, "our-brand"),
                alert(7L, 402L, "our-brand"),
                alert(99L, 999L, "our-brand")));
        when(fixture.trendMapper.insert(any(MarketingMonitorTrendSnapshotDO.class))).thenAnswer(invocation -> {
            invocation.<MarketingMonitorTrendSnapshotDO>getArgument(0).setId(800L);
            return 1;
        });

        MarketingMonitorTrendSnapshotView view = fixture.service.buildTrendSnapshot(7L,
                new MarketingMonitorTrendSnapshotCommand(
                        10L,
                        "DAY",
                        NOW.minusDays(1),
                        NOW,
                        "our-brand",
                        "competitorx",
                        Map.of("trigger", "manual")),
                "operator-1");

        assertThat(view.id()).isEqualTo(800L);
        assertThat(view.mentionCount()).isEqualTo(3);
        assertThat(view.negativeCount()).isEqualTo(1);
        assertThat(view.positiveCount()).isEqualTo(1);
        assertThat(view.neutralCount()).isEqualTo(1);
        assertThat(view.competitorCount()).isEqualTo(1);
        assertThat(view.alertCount()).isEqualTo(2);
        assertThat(view.avgSentimentScore()).isEqualByComparingTo("0.00000");
        ArgumentCaptor<MarketingMonitorTrendSnapshotDO> trendCaptor =
                ArgumentCaptor.forClass(MarketingMonitorTrendSnapshotDO.class);
        verify(fixture.trendMapper).insert(trendCaptor.capture());
        assertThat(trendCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(trendCaptor.getValue().getSourceKey()).isEqualTo("brandwatch");
        assertThat(trendCaptor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void rejectsCrossTenantSourceBeforePolling() {
        Fixture fixture = fixture(new FakePollClient());
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source(99L, 10L, "brandwatch", 1, 1));

        assertThatThrownBy(() -> fixture.service.pollSource(7L, 10L,
                new MarketingMonitorPollCommand(NOW.minusHours(1), NOW, null, 50, false),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monitor source is not found");

        verify(fixture.runMapper, never()).insert(any(MarketingMonitorPollRunDO.class));
    }

    private Fixture fixture(FakePollClient client) {
        MarketingMonitorSourceMapper sourceMapper = mock(MarketingMonitorSourceMapper.class);
        MarketingMonitorItemMapper itemMapper = mock(MarketingMonitorItemMapper.class);
        MarketingSentimentAnalysisMapper sentimentMapper = mock(MarketingSentimentAnalysisMapper.class);
        MarketingCompetitorMentionMapper competitorMapper = mock(MarketingCompetitorMentionMapper.class);
        MarketingMonitorAlertMapper alertMapper = mock(MarketingMonitorAlertMapper.class);
        MarketingMonitorPollRunMapper runMapper = mock(MarketingMonitorPollRunMapper.class);
        MarketingMonitorTrendSnapshotMapper trendMapper = mock(MarketingMonitorTrendSnapshotMapper.class);
        MarketingMonitoringService monitoringService = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService service = new MarketingMonitorPollingService(
                sourceMapper,
                itemMapper,
                sentimentMapper,
                competitorMapper,
                alertMapper,
                runMapper,
                trendMapper,
                monitoringService,
                List.of(client),
                new ObjectMapper(),
                CLOCK);
        return new Fixture(
                sourceMapper,
                itemMapper,
                sentimentMapper,
                competitorMapper,
                alertMapper,
                runMapper,
                trendMapper,
                monitoringService,
                service);
    }

    private MarketingMonitorSourceDO source(Long tenantId,
                                            Long id,
                                            String sourceKey,
                                            Integer enabled,
                                            Integer pollEnabled) {
        MarketingMonitorSourceDO row = new MarketingMonitorSourceDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSourceKey(sourceKey);
        row.setSourceType("BRANDWATCH");
        row.setDisplayName("Brandwatch");
        row.setEnabled(enabled);
        row.setMetadataJson("{\"competitors\":{\"competitorx\":[\"CompetitorX\"]}}");
        row.setPollEnabled(pollEnabled);
        row.setPollIntervalMinutes(60);
        row.setCreatedBy("operator-1");
        row.setCreatedAt(NOW.minusDays(1));
        row.setUpdatedAt(NOW.minusHours(1));
        return row;
    }

    private MarketingMonitorPollItem pollItem(String externalId, String text, int minuteOffset) {
        return new MarketingMonitorPollItem(
                externalId,
                "https://example.test/" + externalId,
                "author-" + minuteOffset,
                "our-brand",
                text,
                "en",
                NOW.minusMinutes(minuteOffset),
                Map.of("rank", minuteOffset));
    }

    private MarketingMonitorItemDO item(Long tenantId, Long id, String externalItemId) {
        MarketingMonitorItemDO row = new MarketingMonitorItemDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSourceId(10L);
        row.setExternalItemId(externalItemId);
        row.setSourceType("BRANDWATCH");
        row.setSourceUrl("https://example.test/" + externalItemId);
        row.setAuthorKey("author-1");
        row.setBrandKey("our-brand");
        row.setTextContent("CompetitorX has bad support");
        row.setLanguage("en");
        row.setPublishedAt(NOW.minusMinutes(10));
        row.setIngestedAt(NOW);
        row.setRawPayloadJson("{}");
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }

    private MarketingMonitorIngestResult ingestResult(String externalItemId, int alertCount) {
        MarketingMonitorItemView item = new MarketingMonitorItemView(
                101L,
                7L,
                10L,
                externalItemId,
                "BRANDWATCH",
                "https://example.test/" + externalItemId,
                "author-2",
                "our-brand",
                "CompetitorX has bad support",
                "en",
                NOW.minusMinutes(2),
                NOW,
                Map.of("rank", 2),
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                List.of("competitorx"));
        MarketingSentimentAnalysisView sentiment = new MarketingSentimentAnalysisView(
                201L,
                7L,
                101L,
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                MarketingMonitoringService.SENTIMENT_MODEL_KEY,
                "lexicon_v1",
                Map.of("negative", List.of("bad"), "positive", List.of()),
                NOW);
        List<MarketingMonitorAlertView> alerts = alertCount == 0
                ? List.of()
                : List.of(new MarketingMonitorAlertView(
                        401L,
                        7L,
                        "NEGATIVE_SENTIMENT",
                        "HIGH",
                        "OPEN",
                        "our-brand",
                        "Negative sentiment detected",
                        "Detected negative sentiment",
                        1,
                        NOW,
                        NOW,
                        Map.of("itemId", 101L),
                        "operator-1",
                        null,
                        null,
                        NOW,
                        NOW));
        return new MarketingMonitorIngestResult(item, sentiment, List.of(), alerts);
    }

    private MarketingSentimentAnalysisDO sentiment(Long tenantId, Long itemId, String label, String score) {
        MarketingSentimentAnalysisDO row = new MarketingSentimentAnalysisDO();
        row.setId(itemId + 2000);
        row.setTenantId(tenantId);
        row.setItemId(itemId);
        row.setSentimentLabel(label);
        row.setSentimentScore(new BigDecimal(score));
        row.setConfidence(new BigDecimal("0.80000"));
        row.setModelKey(MarketingMonitoringService.SENTIMENT_MODEL_KEY);
        row.setModelVersion("lexicon_v1");
        row.setKeywordHitsJson("{}");
        row.setCreatedAt(NOW);
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
        row.setSentimentScore(new BigDecimal("-1.00000"));
        row.setCreatedAt(NOW);
        return row;
    }

    private MarketingMonitorAlertDO alert(Long tenantId, Long id, String scopeKey) {
        MarketingMonitorAlertDO row = new MarketingMonitorAlertDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAlertType("NEGATIVE_SENTIMENT");
        row.setSeverity("HIGH");
        row.setStatus("OPEN");
        row.setScopeKey(scopeKey);
        row.setTitle("Negative sentiment detected");
        row.setReason("Detected negative sentiment");
        row.setItemCount(1);
        row.setWindowStart(NOW.minusHours(1));
        row.setWindowEnd(NOW);
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(NOW.minusMinutes(1));
        row.setUpdatedAt(NOW.minusMinutes(1));
        return row;
    }

    private static final class FakePollClient implements MarketingMonitorPollClient {
        private final MarketingMonitorPollResponse response;
        private final RuntimeException failure;
        private MarketingMonitorPollRequest request;

        private FakePollClient() {
            this(new MarketingMonitorPollResponse(List.of(), null, Map.of()));
        }

        private FakePollClient(MarketingMonitorPollResponse response) {
            this.response = response;
            this.failure = null;
        }

        private FakePollClient(RuntimeException failure) {
            this.response = null;
            this.failure = failure;
        }

        @Override
        public boolean supports(String sourceType) {
            return "BRANDWATCH".equalsIgnoreCase(sourceType);
        }

        @Override
        public MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        MarketingMonitorPollRequest request() {
            return request;
        }
    }

    private record Fixture(
            MarketingMonitorSourceMapper sourceMapper,
            MarketingMonitorItemMapper itemMapper,
            MarketingSentimentAnalysisMapper sentimentMapper,
            MarketingCompetitorMentionMapper competitorMapper,
            MarketingMonitorAlertMapper alertMapper,
            MarketingMonitorPollRunMapper runMapper,
            MarketingMonitorTrendSnapshotMapper trendMapper,
            MarketingMonitoringService monitoringService,
            MarketingMonitorPollingService service) {
    }
}
