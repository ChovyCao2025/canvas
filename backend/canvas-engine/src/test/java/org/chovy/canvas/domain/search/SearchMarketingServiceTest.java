package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
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

class SearchMarketingServiceTest {

    @Test
    void upsertsSourceWithNormalizedProviderChannelAndMetadata() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        doAnswer(invocation -> {
            invocation.<SearchMarketingSourceDO>getArgument(0).setId(10L);
            return 1;
        }).when(sourceMapper).insert(any(SearchMarketingSourceDO.class));
        SearchMarketingService service = service(sourceMapper);

        SearchMarketingSourceView view = service.upsertSource(7L, sourceCommand(), "operator-1");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.provider()).isEqualTo("GOOGLE_ADS");
        assertThat(view.channel()).isEqualTo("SEM");
        assertThat(view.currency()).isEqualTo("USD");
        ArgumentCaptor<SearchMarketingSourceDO> captor = ArgumentCaptor.forClass(SearchMarketingSourceDO.class);
        verify(sourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getProvider()).isEqualTo("GOOGLE_ADS");
        assertThat(captor.getValue().getSourceKey()).isEqualTo("ads-main");
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
        assertThat(captor.getValue().getMetadataJson()).contains("\"manager\":\"search\"");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void upsertsKeywordWithNormalizedIdentityLandingPageHashAndLabels() {
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        doAnswer(invocation -> {
            invocation.<SearchMarketingKeywordDO>getArgument(0).setId(20L);
            return 1;
        }).when(keywordMapper).insert(any(SearchMarketingKeywordDO.class));
        SearchMarketingService service = service(mock(SearchMarketingSourceMapper.class), keywordMapper,
                mock(SearchMarketingSnapshotMapper.class), mock(SearchMarketingOpportunityMapper.class));

        SearchMarketingKeywordView view = service.upsertKeyword(7L, new SearchMarketingKeywordCommand(
                "sem",
                " Running   Shoes ",
                " phrase ",
                " https://example.com/shoes ",
                "commercial",
                List.of("brand", "q3"),
                "active",
                Map.of("owner", "seo-team")), "operator-1");

        assertThat(view.id()).isEqualTo(20L);
        assertThat(view.keywordKey()).isEqualTo("running shoes");
        assertThat(view.matchType()).isEqualTo("PHRASE");
        assertThat(view.landingPageUrlHash()).isEqualTo(sha256("https://example.com/shoes"));
        assertThat(view.labels()).containsExactly("brand", "q3");
        ArgumentCaptor<SearchMarketingKeywordDO> captor = ArgumentCaptor.forClass(SearchMarketingKeywordDO.class);
        verify(keywordMapper).insert(captor.capture());
        assertThat(captor.getValue().getKeywordText()).isEqualTo("Running Shoes");
        assertThat(captor.getValue().getLabelsJson()).contains("brand", "q3");
    }

    @Test
    void rejectsSnapshotWhenSourceBelongsToAnotherTenant() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        SearchMarketingSourceDO source = source();
        source.setTenantId(99L);
        when(sourceMapper.selectById(10L)).thenReturn(source);
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        SearchMarketingService service = service(sourceMapper, keywordMapper, snapshotMapper,
                mock(SearchMarketingOpportunityMapper.class));

        assertThatThrownBy(() -> service.recordSnapshot(7L, snapshotCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");

        verify(snapshotMapper, never()).insert(any(SearchMarketingSnapshotDO.class));
        verify(snapshotMapper, never()).updateById(any(SearchMarketingSnapshotDO.class));
    }

    @Test
    void summaryAggregatesSearchMetricsWithTenantScope() {
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, 10L, 20L, "SEM", date(), 1000L, 40L,
                        new BigDecimal("100.0000"), 4L, new BigDecimal("400.0000"), new BigDecimal("2.5000")),
                snapshot(7L, 10L, 20L, "SEM", date().plusDays(1), 500L, 10L,
                        new BigDecimal("50.0000"), 1L, new BigDecimal("100.0000"), new BigDecimal("3.0000")),
                snapshot(99L, 10L, 20L, "SEM", date(), 9999L, 999L,
                        new BigDecimal("999.0000"), 99L, new BigDecimal("9999.0000"), BigDecimal.ONE)));
        SearchMarketingService service = service(mock(SearchMarketingSourceMapper.class),
                mock(SearchMarketingKeywordMapper.class), snapshotMapper, mock(SearchMarketingOpportunityMapper.class));

        SearchMarketingSummaryView summary = service.summary(7L, new SearchMarketingSummaryQuery(
                "sem",
                10L,
                20L,
                date(),
                date().plusDays(1)));

        assertThat(summary.impressionCount()).isEqualTo(1500L);
        assertThat(summary.clickCount()).isEqualTo(50L);
        assertThat(summary.costAmount()).isEqualByComparingTo("150.0000");
        assertThat(summary.conversionCount()).isEqualTo(5L);
        assertThat(summary.revenueAmount()).isEqualByComparingTo("500.0000");
        assertThat(summary.ctr()).isEqualByComparingTo("0.033333");
        assertThat(summary.cpc()).isEqualByComparingTo("3.000000");
        assertThat(summary.conversionRate()).isEqualByComparingTo("0.100000");
        assertThat(summary.roas()).isEqualByComparingTo("3.333333");
        assertThat(summary.averagePosition()).isEqualByComparingTo("2.666667");
    }

    @Test
    void evaluatesLowCtrSeoPageTwoAndWastedSpendOpportunities() {
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, 10L, 20L, "SEM", date(), 1000L, 5L,
                        new BigDecimal("20.0000"), 1L, new BigDecimal("30.0000"), new BigDecimal("4.0000")),
                snapshot(7L, 10L, 21L, "SEM", date(), 300L, 10L,
                        new BigDecimal("200.0000"), 0L, BigDecimal.ZERO, new BigDecimal("3.0000")),
                snapshot(7L, 11L, 22L, "SEO", date(), 900L, 20L,
                        BigDecimal.ZERO, 0L, BigDecimal.ZERO, new BigDecimal("12.0000")),
                snapshot(99L, 10L, 23L, "SEM", date(), 900L, 1L,
                        new BigDecimal("999.0000"), 0L, BigDecimal.ZERO, new BigDecimal("99.0000"))));
        doAnswer(invocation -> {
            invocation.<SearchMarketingOpportunityDO>getArgument(0).setId(100L);
            return 1;
        }).when(opportunityMapper).insert(any(SearchMarketingOpportunityDO.class));
        SearchMarketingService service = service(mock(SearchMarketingSourceMapper.class),
                mock(SearchMarketingKeywordMapper.class), snapshotMapper, opportunityMapper);

        List<SearchMarketingOpportunityView> opportunities = service.evaluateOpportunities(7L,
                new SearchMarketingOpportunityEvaluationCommand(
                        null,
                        null,
                        null,
                        date(),
                        date(),
                        100L,
                        new BigDecimal("0.010000"),
                        new BigDecimal("10.0000"),
                        new BigDecimal("100.0000")), "operator-1");

        assertThat(opportunities).extracting(SearchMarketingOpportunityView::opportunityType)
                .containsExactlyInAnyOrder("LOW_CTR", "WASTED_SPEND", "SEO_PAGE_TWO");
        ArgumentCaptor<SearchMarketingOpportunityDO> captor =
                ArgumentCaptor.forClass(SearchMarketingOpportunityDO.class);
        verify(opportunityMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(row -> {
            assertThat(row.getTenantId()).isEqualTo(7L);
            assertThat(row.getStatus()).isEqualTo("OPEN");
            assertThat(row.getEvidenceJson()).contains("ctr", "impressionCount");
        });
    }

    @Test
    void reEvaluatingSameEvidenceUpdatesExistingOpportunityWithoutDuplicateOpenRows() {
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, 10L, 20L, "SEM", date(), 1000L, 5L,
                        new BigDecimal("20.0000"), 1L, new BigDecimal("30.0000"), new BigDecimal("4.0000"))));
        SearchMarketingOpportunityDO existing = opportunity(40L, "OPEN");
        when(opportunityMapper.selectOne(any())).thenReturn(existing);
        SearchMarketingService service = service(mock(SearchMarketingSourceMapper.class),
                mock(SearchMarketingKeywordMapper.class), snapshotMapper, opportunityMapper);

        List<SearchMarketingOpportunityView> opportunities = service.evaluateOpportunities(7L,
                new SearchMarketingOpportunityEvaluationCommand(
                        "SEM",
                        10L,
                        20L,
                        date(),
                        date(),
                        100L,
                        new BigDecimal("0.010000"),
                        new BigDecimal("10.0000"),
                        new BigDecimal("100.0000")), "operator-1");

        assertThat(opportunities).singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo(40L));
        verify(opportunityMapper, never()).insert(any(SearchMarketingOpportunityDO.class));
        verify(opportunityMapper).updateById(existing);
    }

    @Test
    void operatorCanMarkOpportunityLifecycleStatus() {
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingOpportunityDO row = opportunity(40L, "OPEN");
        when(opportunityMapper.selectById(40L)).thenReturn(row);
        SearchMarketingService service = service(mock(SearchMarketingSourceMapper.class),
                mock(SearchMarketingKeywordMapper.class), mock(SearchMarketingSnapshotMapper.class), opportunityMapper);

        SearchMarketingOpportunityView view = service.updateOpportunityStatus(7L, 40L,
                new SearchMarketingOpportunityStatusCommand("rollback_required", "ROAS regressed"), "operator-1");

        assertThat(view.status()).isEqualTo("ROLLBACK_REQUIRED");
        assertThat(view.evidence()).containsEntry("statusReason", "ROAS regressed");
        verify(opportunityMapper).updateById(row);
    }

    private SearchMarketingService service(SearchMarketingSourceMapper sourceMapper) {
        return service(sourceMapper, mock(SearchMarketingKeywordMapper.class), mock(SearchMarketingSnapshotMapper.class),
                mock(SearchMarketingOpportunityMapper.class));
    }

    private SearchMarketingService service(SearchMarketingSourceMapper sourceMapper,
                                           SearchMarketingKeywordMapper keywordMapper,
                                           SearchMarketingSnapshotMapper snapshotMapper,
                                           SearchMarketingOpportunityMapper opportunityMapper) {
        return new SearchMarketingService(
                sourceMapper,
                keywordMapper,
                snapshotMapper,
                opportunityMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private SearchMarketingSourceCommand sourceCommand() {
        return new SearchMarketingSourceCommand(
                "google_ads",
                "ads-main",
                "Google Ads Main",
                "sem",
                "123-456",
                null,
                "usd",
                "Asia/Shanghai",
                true,
                Map.of("manager", "search"));
    }

    private SearchMarketingSnapshotCommand snapshotCommand() {
        return new SearchMarketingSnapshotCommand(
                10L,
                20L,
                date(),
                "desktop",
                "US",
                "brand",
                1000L,
                40L,
                new BigDecimal("100.0000"),
                4L,
                new BigDecimal("400.0000"),
                new BigDecimal("2.5000"),
                Map.of("source", "import"));
    }

    private SearchMarketingSourceDO source() {
        SearchMarketingSourceDO row = new SearchMarketingSourceDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("GOOGLE_ADS");
        row.setSourceKey("ads-main");
        row.setDisplayName("Google Ads Main");
        row.setChannel("SEM");
        row.setCurrency("USD");
        row.setTimezone("Asia/Shanghai");
        row.setEnabled(1);
        return row;
    }

    private SearchMarketingKeywordDO keyword() {
        SearchMarketingKeywordDO row = new SearchMarketingKeywordDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setChannel("SEM");
        row.setKeywordText("Running Shoes");
        row.setKeywordKey("running shoes");
        row.setMatchType("PHRASE");
        row.setLandingPageUrl("https://example.com/shoes");
        row.setLandingPageUrlHash(sha256("https://example.com/shoes"));
        row.setStatus("ACTIVE");
        return row;
    }

    private SearchMarketingSnapshotDO snapshot(Long tenantId,
                                               Long sourceId,
                                               Long keywordId,
                                               String channel,
                                               LocalDate snapshotDate,
                                               Long impressions,
                                               Long clicks,
                                               BigDecimal cost,
                                               Long conversions,
                                               BigDecimal revenue,
                                               BigDecimal averagePosition) {
        SearchMarketingSnapshotDO row = new SearchMarketingSnapshotDO();
        row.setId(keywordId);
        row.setTenantId(tenantId);
        row.setSourceId(sourceId);
        row.setKeywordId(keywordId);
        row.setChannel(channel);
        row.setSnapshotDate(snapshotDate);
        row.setDevice("ALL");
        row.setCountry("ALL");
        row.setQueryGroupKey("DEFAULT");
        row.setImpressionCount(impressions);
        row.setClickCount(clicks);
        row.setCostAmount(cost);
        row.setConversionCount(conversions);
        row.setRevenueAmount(revenue);
        row.setAveragePosition(averagePosition);
        return row;
    }

    private SearchMarketingOpportunityDO opportunity(Long id, String status) {
        SearchMarketingOpportunityDO row = new SearchMarketingOpportunityDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setKeywordId(20L);
        row.setChannel("SEM");
        row.setOpportunityType("LOW_CTR");
        row.setSnapshotDate(date());
        row.setSeverity("HIGH");
        row.setStatus(status);
        row.setRecommendation("Improve CTR");
        row.setImpactScore(new BigDecimal("5.0000"));
        row.setEvidenceJson("{\"ctr\":\"0.005000\"}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 6, 0, 0));
        row.setUpdatedAt(java.time.LocalDateTime.of(2026, 6, 6, 0, 0));
        return row;
    }

    private LocalDate date() {
        return LocalDate.of(2026, 6, 6);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
