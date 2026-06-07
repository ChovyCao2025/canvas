package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingImpactWindowDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingUrlInspectionDO;
import org.chovy.canvas.dal.mapper.SearchMarketingImpactWindowMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingUrlInspectionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingImpactWindowServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void reconciledMutationSchedulesPostChangeImpactWindow() {
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingImpactWindowMapper impactWindowMapper = mock(SearchMarketingImpactWindowMapper.class);
        when(mutationMapper.selectById(50L)).thenReturn(mutation("RECONCILED"));
        when(opportunityMapper.selectById(40L)).thenReturn(opportunity("SEM"));
        doAnswer(invocation -> {
            invocation.<SearchMarketingImpactWindowDO>getArgument(0).setId(70L);
            return 1;
        }).when(impactWindowMapper).insert(any(SearchMarketingImpactWindowDO.class));

        SearchMarketingImpactWindowView view = service(mutationMapper, opportunityMapper, impactWindowMapper,
                mock(SearchMarketingSnapshotMapper.class), mock(SearchMarketingUrlInspectionMapper.class))
                .scheduleForReconciledMutation(7L, 50L, "operator-1");

        assertThat(view.status()).isEqualTo("SCHEDULED");
        assertThat(view.baselineStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(view.baselineEndDate()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(view.postStartDate()).isEqualTo(LocalDate.of(2026, 6, 9));
        assertThat(view.postEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(view.dueAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 0, 0));
        ArgumentCaptor<SearchMarketingImpactWindowDO> windowCaptor =
                ArgumentCaptor.forClass(SearchMarketingImpactWindowDO.class);
        verify(impactWindowMapper).insert(windowCaptor.capture());
        assertThat(windowCaptor.getValue().getOpportunityId()).isEqualTo(40L);
        assertThat(windowCaptor.getValue().getMutationId()).isEqualTo(50L);
    }

    @Test
    void positivePostWindowMovementClosesOpportunityAsImpactPositive() {
        Fixture fixture = fixture("SEM");
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(LocalDate.of(2026, 6, 3), 10L, 20L, 1000, 40, "100.0000", 4, "400.0000"),
                snapshot(LocalDate.of(2026, 6, 10), 10L, 20L, 1000, 80, "110.0000", 12, "1320.0000"),
                snapshot(LocalDate.of(2026, 6, 10), 11L, 20L, 1000, 10, "10.0000", 0, "0.0000")));

        SearchMarketingImpactWindowView view = fixture.service.evaluateDue(7L, 10, "operator-1").getFirst();

        assertThat(view.decision()).isEqualTo("IMPACT_POSITIVE");
        assertThat(view.metricDeltas()).containsEntry("baselineSnapshotCount", 1);
        assertThat(view.metricDeltas()).containsEntry("postSnapshotCount", 1);
        assertThat(fixture.opportunity.getStatus()).isEqualTo("IMPACT_POSITIVE");
    }

    @Test
    void spendIncreaseWithoutConversionsRequiresRollback() {
        Fixture fixture = fixture("SEM");
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(LocalDate.of(2026, 6, 3), 10L, 20L, 1000, 40, "100.0000", 4, "400.0000"),
                snapshot(LocalDate.of(2026, 6, 10), 10L, 20L, 1000, 30, "130.0000", 0, "0.0000")));

        SearchMarketingImpactWindowView view = fixture.service.evaluateDue(7L, 10, "operator-1").getFirst();

        assertThat(view.decision()).isEqualTo("ROLLBACK_REQUIRED");
        assertThat(((Number) view.metricDeltas().get("postConversions")).longValue()).isZero();
        assertThat(fixture.opportunity.getStatus()).isEqualTo("ROLLBACK_REQUIRED");
    }

    @Test
    void seoIndexedStateRegressionRequiresRollback() {
        Fixture fixture = fixture("SEO");
        when(fixture.snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(LocalDate.of(2026, 6, 3), 10L, 20L, 1000, 80, "0.0000", 0, "0.0000"),
                snapshot(LocalDate.of(2026, 6, 10), 10L, 20L, 1000, 70, "0.0000", 0, "0.0000")));
        when(fixture.urlInspectionMapper.selectList(any())).thenReturn(List.of(
                inspection(LocalDate.of(2026, 6, 3), "INDEXED"),
                inspection(LocalDate.of(2026, 6, 10), "NOT_INDEXED")));

        SearchMarketingImpactWindowView view = fixture.service.evaluateDue(7L, 10, "operator-1").getFirst();

        assertThat(view.decision()).isEqualTo("ROLLBACK_REQUIRED");
        assertThat(view.metricDeltas()).containsEntry("baselineIndexedState", "INDEXED");
        assertThat(view.metricDeltas()).containsEntry("postIndexedState", "NOT_INDEXED");
    }

    private Fixture fixture(String channel) {
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingImpactWindowMapper impactWindowMapper = mock(SearchMarketingImpactWindowMapper.class);
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        SearchMarketingUrlInspectionMapper urlInspectionMapper = mock(SearchMarketingUrlInspectionMapper.class);
        SearchMarketingImpactWindowDO window = window(channel);
        SearchMarketingOpportunityDO opportunity = opportunity(channel);
        when(impactWindowMapper.selectList(any())).thenReturn(List.of(window));
        when(opportunityMapper.selectById(40L)).thenReturn(opportunity);
        return new Fixture(
                service(mutationMapper, opportunityMapper, impactWindowMapper, snapshotMapper, urlInspectionMapper),
                snapshotMapper,
                urlInspectionMapper,
                opportunity);
    }

    private SearchMarketingImpactWindowService service(SearchMarketingMutationMapper mutationMapper,
                                                       SearchMarketingOpportunityMapper opportunityMapper,
                                                       SearchMarketingImpactWindowMapper impactWindowMapper,
                                                       SearchMarketingSnapshotMapper snapshotMapper,
                                                       SearchMarketingUrlInspectionMapper urlInspectionMapper) {
        return new SearchMarketingImpactWindowService(mutationMapper, opportunityMapper, impactWindowMapper,
                snapshotMapper, urlInspectionMapper, new ObjectMapper(), CLOCK);
    }

    private SearchMarketingMutationDO mutation(String status) {
        SearchMarketingMutationDO row = new SearchMarketingMutationDO();
        row.setId(50L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setOpportunityId(40L);
        row.setKeywordId(20L);
        row.setStatus(status);
        row.setExecutedAt(LocalDateTime.of(2026, 6, 8, 12, 0));
        return row;
    }

    private SearchMarketingOpportunityDO opportunity(String channel) {
        SearchMarketingOpportunityDO row = new SearchMarketingOpportunityDO();
        row.setId(40L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setKeywordId(20L);
        row.setChannel(channel);
        row.setSnapshotDate(LocalDate.of(2026, 6, 7));
        row.setStatus("OPEN");
        row.setEvidenceJson("{}");
        return row;
    }

    private SearchMarketingImpactWindowDO window(String channel) {
        SearchMarketingImpactWindowDO row = new SearchMarketingImpactWindowDO();
        row.setId(70L);
        row.setTenantId(7L);
        row.setOpportunityId(40L);
        row.setMutationId(50L);
        row.setSourceId(10L);
        row.setKeywordId(20L);
        row.setPageUrlHash("page-hash");
        row.setBaselineStartDate(LocalDate.of(2026, 6, 1));
        row.setBaselineEndDate(LocalDate.of(2026, 6, 7));
        row.setPostStartDate(LocalDate.of(2026, 6, 9));
        row.setPostEndDate(LocalDate.of(2026, 6, 15));
        row.setStatus("SCHEDULED");
        row.setEvidenceJson("{\"channel\":\"" + channel + "\"}");
        row.setDueAt(LocalDateTime.of(2026, 6, 16, 0, 0));
        return row;
    }

    private SearchMarketingSnapshotDO snapshot(LocalDate date,
                                               Long sourceId,
                                               Long keywordId,
                                               long impressions,
                                               long clicks,
                                               String cost,
                                               long conversions,
                                               String revenue) {
        SearchMarketingSnapshotDO row = new SearchMarketingSnapshotDO();
        row.setTenantId(7L);
        row.setSourceId(sourceId);
        row.setKeywordId(keywordId);
        row.setSnapshotDate(date);
        row.setImpressionCount(impressions);
        row.setClickCount(clicks);
        row.setCostAmount(new BigDecimal(cost));
        row.setConversionCount(conversions);
        row.setRevenueAmount(new BigDecimal(revenue));
        row.setAveragePosition(new BigDecimal("2.5000"));
        return row;
    }

    private SearchMarketingUrlInspectionDO inspection(LocalDate date, String indexedState) {
        SearchMarketingUrlInspectionDO row = new SearchMarketingUrlInspectionDO();
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setPageUrlHash("page-hash");
        row.setInspectionDate(date);
        row.setIndexedState(indexedState);
        return row;
    }

    private record Fixture(SearchMarketingImpactWindowService service,
                           SearchMarketingSnapshotMapper snapshotMapper,
                           SearchMarketingUrlInspectionMapper urlInspectionMapper,
                           SearchMarketingOpportunityDO opportunity) {
    }
}
