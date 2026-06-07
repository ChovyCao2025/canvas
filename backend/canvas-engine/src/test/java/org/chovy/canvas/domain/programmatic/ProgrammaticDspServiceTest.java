package org.chovy.canvas.domain.programmatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspPerformanceSnapshotDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO;
import org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspPerformanceSnapshotMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

class ProgrammaticDspServiceTest {

    @Test
    void upsertsSeatWithNormalizedProviderCurrencyEnforcementAndMetadata() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        doAnswer(invocation -> {
            invocation.<ProgrammaticDspSeatDO>getArgument(0).setId(10L);
            return 1;
        }).when(seatMapper).insert(any(ProgrammaticDspSeatDO.class));
        ProgrammaticDspService service = service(seatMapper);

        ProgrammaticDspSeatView view = service.upsertSeat(7L, new ProgrammaticDspSeatCommand(
                "the_trade_desk",
                " seat-main ",
                "Main DSP Seat",
                "adv-77",
                "usd",
                null,
                " enforce ",
                true,
                Map.of("owner", "paid-media")), "operator-1");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.provider()).isEqualTo("THE_TRADE_DESK");
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.timezone()).isEqualTo("UTC");
        assertThat(view.supplyChainEnforcement()).isEqualTo("ENFORCE");
        assertThat(view.enabled()).isTrue();
        assertThat(view.metadata()).containsEntry("owner", "paid-media");
        ArgumentCaptor<ProgrammaticDspSeatDO> captor = ArgumentCaptor.forClass(ProgrammaticDspSeatDO.class);
        verify(seatMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getProvider()).isEqualTo("THE_TRADE_DESK");
        assertThat(captor.getValue().getSeatKey()).isEqualTo("seat-main");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
        assertThat(captor.getValue().getMetadataJson()).contains("\"owner\":\"paid-media\"");
    }

    @Test
    void upsertsCampaignByTenantCampaignKey() {
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        doAnswer(invocation -> {
            invocation.<ProgrammaticDspCampaignDO>getArgument(0).setId(20L);
            return 1;
        }).when(campaignMapper).insert(any(ProgrammaticDspCampaignDO.class));
        ProgrammaticDspService service = service(mock(ProgrammaticDspSeatMapper.class), campaignMapper,
                mock(ProgrammaticDspLineItemMapper.class), mock(ProgrammaticDspSupplyPathMapper.class),
                mock(ProgrammaticDspPerformanceSnapshotMapper.class));

        ProgrammaticDspCampaignView view = service.upsertCampaign(7L, new ProgrammaticDspCampaignCommand(
                "summer-2026",
                "Summer 2026",
                "awareness",
                new BigDecimal("5000.0000"),
                "usd",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59),
                "active",
                Map.of("brief", "upper-funnel")), "operator-1");

        assertThat(view.id()).isEqualTo(20L);
        assertThat(view.campaignKey()).isEqualTo("summer-2026");
        assertThat(view.objective()).isEqualTo("AWARENESS");
        assertThat(view.budgetAmount()).isEqualByComparingTo("5000.0000");
        assertThat(view.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<ProgrammaticDspCampaignDO> captor =
                ArgumentCaptor.forClass(ProgrammaticDspCampaignDO.class);
        verify(campaignMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getCampaignKey()).isEqualTo("summer-2026");
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
    }

    @Test
    void rejectsLineItemWhenSeatBelongsToAnotherTenant() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSeatDO seat = seat(99L);
        when(seatMapper.selectById(10L)).thenReturn(seat);
        when(campaignMapper.selectById(20L)).thenReturn(campaign(7L));
        ProgrammaticDspService service = service(seatMapper, campaignMapper, lineItemMapper,
                mock(ProgrammaticDspSupplyPathMapper.class), mock(ProgrammaticDspPerformanceSnapshotMapper.class));

        assertThatThrownBy(() -> service.upsertLineItem(7L, lineItemCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seat");

        verify(lineItemMapper, never()).insert(any(ProgrammaticDspLineItemDO.class));
        verify(lineItemMapper, never()).updateById(any(ProgrammaticDspLineItemDO.class));
    }

    @Test
    void recordsSupplyPathAndSnapshotForTenantOwnedLineItem() {
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspPerformanceSnapshotMapper snapshotMapper = mock(ProgrammaticDspPerformanceSnapshotMapper.class);
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem(7L));
        doAnswer(invocation -> {
            invocation.<ProgrammaticDspSupplyPathDO>getArgument(0).setId(40L);
            return 1;
        }).when(supplyPathMapper).insert(any(ProgrammaticDspSupplyPathDO.class));
        doAnswer(invocation -> {
            invocation.<ProgrammaticDspPerformanceSnapshotDO>getArgument(0).setId(50L);
            return 1;
        }).when(snapshotMapper).insert(any(ProgrammaticDspPerformanceSnapshotDO.class));
        ProgrammaticDspService service = service(mock(ProgrammaticDspSeatMapper.class),
                mock(ProgrammaticDspCampaignMapper.class), lineItemMapper, supplyPathMapper, snapshotMapper);

        ProgrammaticDspSupplyPathView supplyPath = service.upsertSupplyPath(7L, new ProgrammaticDspSupplyPathCommand(
                30L,
                "openx",
                "deal-1",
                "seller-9",
                "publisher.example",
                "video",
                "authorized",
                "matched",
                true,
                "active",
                Map.of("inventory", "ctv")), "operator-1");
        ProgrammaticDspSnapshotView snapshot = service.recordSnapshot(7L, new ProgrammaticDspSnapshotCommand(
                30L,
                LocalDate.of(2026, 6, 6),
                1000L,
                200L,
                500L,
                25L,
                5L,
                400L,
                new BigDecimal("250.0000"),
                new BigDecimal("900.0000"),
                Map.of("source", "daily-import")), "operator-1");

        assertThat(supplyPath.id()).isEqualTo(40L);
        assertThat(supplyPath.exchangeKey()).isEqualTo("OPENX");
        assertThat(supplyPath.adsTxtStatus()).isEqualTo("AUTHORIZED");
        assertThat(supplyPath.schainComplete()).isTrue();
        assertThat(snapshot.id()).isEqualTo(50L);
        assertThat(snapshot.seatId()).isEqualTo(10L);
        assertThat(snapshot.campaignId()).isEqualTo(20L);
        assertThat(snapshot.spendAmount()).isEqualByComparingTo("250.0000");
    }

    @Test
    void summaryAggregatesTenantScopedSnapshotsAndComputesRatesAndPacing() {
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspPerformanceSnapshotMapper snapshotMapper = mock(ProgrammaticDspPerformanceSnapshotMapper.class);
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem(7L));
        when(campaignMapper.selectById(20L)).thenReturn(campaign(7L));
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(7L, 10L, 20L, 30L, date(), 1000L, 200L, 500L, 20L, 4L, 400L,
                        new BigDecimal("200.0000"), new BigDecimal("1000.0000")),
                snapshot(7L, 10L, 20L, 30L, date().plusDays(1), 500L, 100L, 500L, 30L, 6L, 350L,
                        new BigDecimal("300.0000"), new BigDecimal("500.0000")),
                snapshot(99L, 10L, 20L, 30L, date(), 9999L, 9999L, 9999L, 999L, 99L, 999L,
                        new BigDecimal("999.0000"), new BigDecimal("9999.0000"))));
        ProgrammaticDspService service = service(mock(ProgrammaticDspSeatMapper.class), campaignMapper,
                lineItemMapper, mock(ProgrammaticDspSupplyPathMapper.class), snapshotMapper);

        ProgrammaticDspSummaryView summary = service.summary(7L, new ProgrammaticDspSummaryQuery(
                10L,
                20L,
                30L,
                date(),
                date().plusDays(1),
                LocalDateTime.of(2026, 6, 15, 0, 0)));

        assertThat(summary.snapshotCount()).isEqualTo(2);
        assertThat(summary.bidCount()).isEqualTo(1500L);
        assertThat(summary.winCount()).isEqualTo(300L);
        assertThat(summary.impressionCount()).isEqualTo(1000L);
        assertThat(summary.clickCount()).isEqualTo(50L);
        assertThat(summary.conversionCount()).isEqualTo(10L);
        assertThat(summary.viewableImpressionCount()).isEqualTo(750L);
        assertThat(summary.spendAmount()).isEqualByComparingTo("500.0000");
        assertThat(summary.revenueAmount()).isEqualByComparingTo("1500.0000");
        assertThat(summary.budgetAmount()).isEqualByComparingTo("1000.0000");
        assertThat(summary.winRate()).isEqualByComparingTo("0.200000");
        assertThat(summary.ctr()).isEqualByComparingTo("0.050000");
        assertThat(summary.conversionRate()).isEqualByComparingTo("0.200000");
        assertThat(summary.cpa()).isEqualByComparingTo("50.000000");
        assertThat(summary.roas()).isEqualByComparingTo("3.000000");
        assertThat(summary.viewabilityRate()).isEqualByComparingTo("0.750000");
        assertThat(summary.budgetSpentPercent()).isEqualByComparingTo("0.500000");
        assertThat(summary.pacingStatus()).isEqualTo("ON_TRACK");
    }

    private ProgrammaticDspService service(ProgrammaticDspSeatMapper seatMapper) {
        return service(seatMapper, mock(ProgrammaticDspCampaignMapper.class),
                mock(ProgrammaticDspLineItemMapper.class), mock(ProgrammaticDspSupplyPathMapper.class),
                mock(ProgrammaticDspPerformanceSnapshotMapper.class));
    }

    private ProgrammaticDspService service(ProgrammaticDspSeatMapper seatMapper,
                                           ProgrammaticDspCampaignMapper campaignMapper,
                                           ProgrammaticDspLineItemMapper lineItemMapper,
                                           ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                           ProgrammaticDspPerformanceSnapshotMapper snapshotMapper) {
        return new ProgrammaticDspService(
                seatMapper,
                campaignMapper,
                lineItemMapper,
                supplyPathMapper,
                snapshotMapper,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private ProgrammaticDspLineItemCommand lineItemCommand() {
        return new ProgrammaticDspLineItemCommand(
                10L,
                20L,
                "li-main",
                "Main Line Item",
                "auto",
                new BigDecimal("12.5000"),
                new BigDecimal("100.0000"),
                new BigDecimal("1000.0000"),
                "even",
                Map.of("geo", List.of("US")),
                3,
                "active",
                Map.of("goal", "reach"));
    }

    private ProgrammaticDspSeatDO seat(Long tenantId) {
        ProgrammaticDspSeatDO row = new ProgrammaticDspSeatDO();
        row.setId(10L);
        row.setTenantId(tenantId);
        row.setProvider("THE_TRADE_DESK");
        row.setSeatKey("seat-main");
        row.setDisplayName("Main Seat");
        row.setCurrency("USD");
        row.setTimezone("UTC");
        row.setSupplyChainEnforcement("ENFORCE");
        row.setEnabled(1);
        return row;
    }

    private ProgrammaticDspCampaignDO campaign(Long tenantId) {
        ProgrammaticDspCampaignDO row = new ProgrammaticDspCampaignDO();
        row.setId(20L);
        row.setTenantId(tenantId);
        row.setCampaignKey("summer-2026");
        row.setCampaignName("Summer 2026");
        row.setObjective("AWARENESS");
        row.setBudgetAmount(new BigDecimal("5000.0000"));
        row.setCurrency("USD");
        row.setStartAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        row.setEndAt(LocalDateTime.of(2026, 6, 30, 0, 0));
        row.setStatus("ACTIVE");
        return row;
    }

    private ProgrammaticDspLineItemDO lineItem(Long tenantId) {
        ProgrammaticDspLineItemDO row = new ProgrammaticDspLineItemDO();
        row.setId(30L);
        row.setTenantId(tenantId);
        row.setSeatId(10L);
        row.setCampaignId(20L);
        row.setLineItemKey("li-main");
        row.setLineItemName("Main Line Item");
        row.setBidStrategy("AUTO");
        row.setMaxBidCpm(new BigDecimal("12.5000"));
        row.setDailyBudgetAmount(new BigDecimal("100.0000"));
        row.setTotalBudgetAmount(new BigDecimal("1000.0000"));
        row.setPacingMode("EVEN");
        row.setFrequencyCap(3);
        row.setStatus("ACTIVE");
        return row;
    }

    private ProgrammaticDspPerformanceSnapshotDO snapshot(Long tenantId,
                                                          Long seatId,
                                                          Long campaignId,
                                                          Long lineItemId,
                                                          LocalDate snapshotDate,
                                                          Long bids,
                                                          Long wins,
                                                          Long impressions,
                                                          Long clicks,
                                                          Long conversions,
                                                          Long viewableImpressions,
                                                          BigDecimal spend,
                                                          BigDecimal revenue) {
        ProgrammaticDspPerformanceSnapshotDO row = new ProgrammaticDspPerformanceSnapshotDO();
        row.setId(lineItemId);
        row.setTenantId(tenantId);
        row.setSeatId(seatId);
        row.setCampaignId(campaignId);
        row.setLineItemId(lineItemId);
        row.setSnapshotDate(snapshotDate);
        row.setBidCount(bids);
        row.setWinCount(wins);
        row.setImpressionCount(impressions);
        row.setClickCount(clicks);
        row.setConversionCount(conversions);
        row.setViewableImpressionCount(viewableImpressions);
        row.setSpendAmount(spend);
        row.setRevenueAmount(revenue);
        return row;
    }

    private LocalDate date() {
        return LocalDate.of(2026, 6, 6);
    }
}
