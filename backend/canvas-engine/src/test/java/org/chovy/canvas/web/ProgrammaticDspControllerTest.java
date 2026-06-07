package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationApprovalCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationExecuteCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationQuery;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationService;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspService;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryQuery;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgrammaticDspControllerTest {

    @Test
    void writeEndpointsPassCurrentTenantAndOperator() {
        ProgrammaticDspService service = mock(ProgrammaticDspService.class);
        ProgrammaticDspMutationService mutationService = mock(ProgrammaticDspMutationService.class);
        ProgrammaticDspSeatCommand seatCommand = seatCommand();
        ProgrammaticDspCampaignCommand campaignCommand = campaignCommand();
        ProgrammaticDspLineItemCommand lineItemCommand = lineItemCommand();
        ProgrammaticDspSupplyPathCommand supplyPathCommand = supplyPathCommand();
        ProgrammaticDspSnapshotCommand snapshotCommand = snapshotCommand();
        when(service.upsertSeat(7L, seatCommand, "operator-1")).thenReturn(seatView());
        when(service.upsertCampaign(7L, campaignCommand, "operator-1")).thenReturn(campaignView());
        when(service.upsertLineItem(7L, lineItemCommand, "operator-1")).thenReturn(lineItemView());
        when(service.upsertSupplyPath(7L, supplyPathCommand, "operator-1")).thenReturn(supplyPathView());
        when(service.recordSnapshot(7L, snapshotCommand, "operator-1")).thenReturn(snapshotView());
        ProgrammaticDspController controller = new ProgrammaticDspController(service, mutationService, resolver());

        StepVerifier.create(controller.upsertSeat(seatCommand))
                .assertNext(response -> assertThat(response.getData().provider()).isEqualTo("THE_TRADE_DESK"))
                .verifyComplete();
        StepVerifier.create(controller.upsertCampaign(campaignCommand))
                .assertNext(response -> assertThat(response.getData().campaignKey()).isEqualTo("summer-2026"))
                .verifyComplete();
        StepVerifier.create(controller.upsertLineItem(lineItemCommand))
                .assertNext(response -> assertThat(response.getData().lineItemKey()).isEqualTo("li-main"))
                .verifyComplete();
        StepVerifier.create(controller.upsertSupplyPath(supplyPathCommand))
                .assertNext(response -> assertThat(response.getData().exchangeKey()).isEqualTo("OPENX"))
                .verifyComplete();
        StepVerifier.create(controller.recordSnapshot(snapshotCommand))
                .assertNext(response -> assertThat(response.getData().spendAmount()).isEqualByComparingTo("250.0000"))
                .verifyComplete();

        verify(service).upsertSeat(7L, seatCommand, "operator-1");
        verify(service).upsertCampaign(7L, campaignCommand, "operator-1");
        verify(service).upsertLineItem(7L, lineItemCommand, "operator-1");
        verify(service).upsertSupplyPath(7L, supplyPathCommand, "operator-1");
        verify(service).recordSnapshot(7L, snapshotCommand, "operator-1");
    }

    @Test
    void summaryEndpointPassesFilters() {
        ProgrammaticDspService service = mock(ProgrammaticDspService.class);
        ProgrammaticDspMutationService mutationService = mock(ProgrammaticDspMutationService.class);
        ProgrammaticDspSummaryQuery query = new ProgrammaticDspSummaryQuery(
                10L, 20L, 30L, date(), date().plusDays(1), now());
        when(service.summary(7L, query)).thenReturn(summaryView());
        ProgrammaticDspController controller = new ProgrammaticDspController(service, mutationService, resolver());

        StepVerifier.create(controller.summary(10L, 20L, 30L, date(), date().plusDays(1), now()))
                .assertNext(response -> assertThat(response.getData().pacingStatus()).isEqualTo("ON_TRACK"))
                .verifyComplete();

        verify(service).summary(7L, query);
    }

    @Test
    void mutationEndpointsPassCurrentTenantAndOperator() {
        ProgrammaticDspService service = mock(ProgrammaticDspService.class);
        ProgrammaticDspMutationService mutationService = mock(ProgrammaticDspMutationService.class);
        ProgrammaticDspMutationCommand proposeCommand = mutationCommand();
        ProgrammaticDspMutationApprovalCommand approvalCommand =
                new ProgrammaticDspMutationApprovalCommand("APPROVED", "ready");
        ProgrammaticDspMutationExecuteCommand executeCommand =
                new ProgrammaticDspMutationExecuteCommand(true, true, Map.of("validateOnly", true));
        ProgrammaticDspMutationQuery query = new ProgrammaticDspMutationQuery(10L, 20L, 30L, "READY", "APPROVED", 20);
        when(mutationService.propose(7L, proposeCommand, "operator-1")).thenReturn(mutationView("DRAFT"));
        when(mutationService.approve(7L, 70L, approvalCommand, "operator-1")).thenReturn(mutationView("READY"));
        when(mutationService.execute(7L, 70L, executeCommand, "operator-1")).thenReturn(mutationView("DRY_RUN_OK"));
        when(mutationService.list(7L, query)).thenReturn(java.util.List.of(mutationView("READY")));
        ProgrammaticDspController controller = new ProgrammaticDspController(service, mutationService, resolver());

        StepVerifier.create(controller.proposeMutation(proposeCommand))
                .assertNext(response -> assertThat(response.getData().mutationKey()).isEqualTo("li-bid-raise-1"))
                .verifyComplete();
        StepVerifier.create(controller.approveMutation(70L, approvalCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("READY"))
                .verifyComplete();
        StepVerifier.create(controller.executeMutation(70L, executeCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRY_RUN_OK"))
                .verifyComplete();
        StepVerifier.create(controller.listMutations(10L, 20L, 30L, "READY", "APPROVED", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.approvalStatus()).isEqualTo("APPROVED")))
                .verifyComplete();

        verify(mutationService).propose(7L, proposeCommand, "operator-1");
        verify(mutationService).approve(7L, 70L, approvalCommand, "operator-1");
        verify(mutationService).execute(7L, 70L, executeCommand, "operator-1");
        verify(mutationService).list(7L, query);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private ProgrammaticDspSeatCommand seatCommand() {
        return new ProgrammaticDspSeatCommand(
                "THE_TRADE_DESK",
                "seat-main",
                "Main DSP Seat",
                "adv-77",
                "USD",
                "UTC",
                "ENFORCE",
                true,
                Map.of());
    }

    private ProgrammaticDspCampaignCommand campaignCommand() {
        return new ProgrammaticDspCampaignCommand(
                "summer-2026",
                "Summer 2026",
                "AWARENESS",
                new BigDecimal("5000.0000"),
                "USD",
                now().minusDays(5),
                now().plusDays(25),
                "ACTIVE",
                Map.of());
    }

    private ProgrammaticDspLineItemCommand lineItemCommand() {
        return new ProgrammaticDspLineItemCommand(
                10L,
                20L,
                "li-main",
                "Main Line Item",
                "AUTO",
                new BigDecimal("12.5000"),
                new BigDecimal("100.0000"),
                new BigDecimal("1000.0000"),
                "EVEN",
                Map.of("geo", "US"),
                3,
                "ACTIVE",
                Map.of());
    }

    private ProgrammaticDspSupplyPathCommand supplyPathCommand() {
        return new ProgrammaticDspSupplyPathCommand(
                30L,
                "OPENX",
                "deal-1",
                "seller-9",
                "publisher.example",
                "VIDEO",
                "AUTHORIZED",
                "MATCHED",
                true,
                "ACTIVE",
                Map.of());
    }

    private ProgrammaticDspSnapshotCommand snapshotCommand() {
        return new ProgrammaticDspSnapshotCommand(
                30L,
                date(),
                1000L,
                200L,
                500L,
                25L,
                5L,
                400L,
                new BigDecimal("250.0000"),
                new BigDecimal("900.0000"),
                Map.of());
    }

    private ProgrammaticDspMutationCommand mutationCommand() {
        return new ProgrammaticDspMutationCommand(
                10L,
                20L,
                30L,
                40L,
                "li-bid-raise-1",
                "UPDATE_LINE_ITEM_BID",
                "LINE_ITEM",
                "advertisers/1/lineItems/2",
                true,
                "idem-1",
                Map.of("bidCpmMicros", 12500000));
    }

    private ProgrammaticDspSeatView seatView() {
        return new ProgrammaticDspSeatView(10L, 7L, "THE_TRADE_DESK", "seat-main", "Main DSP Seat",
                "adv-77", "USD", "UTC", "ENFORCE", true, Map.of(), "operator-1", now(), now());
    }

    private ProgrammaticDspCampaignView campaignView() {
        return new ProgrammaticDspCampaignView(20L, 7L, "summer-2026", "Summer 2026", "AWARENESS",
                new BigDecimal("5000.0000"), "USD", now().minusDays(5), now().plusDays(25),
                "ACTIVE", Map.of(), "operator-1", now(), now());
    }

    private ProgrammaticDspLineItemView lineItemView() {
        return new ProgrammaticDspLineItemView(30L, 7L, 10L, 20L, "li-main", "Main Line Item",
                "AUTO", new BigDecimal("12.5000"), new BigDecimal("100.0000"),
                new BigDecimal("1000.0000"), "EVEN", Map.of("geo", "US"), 3, "ACTIVE",
                Map.of(), "operator-1", now(), now());
    }

    private ProgrammaticDspSupplyPathView supplyPathView() {
        return new ProgrammaticDspSupplyPathView(40L, 7L, 30L, "OPENX", "deal-1", "seller-9",
                "publisher.example", "VIDEO", "AUTHORIZED", "MATCHED", true, "ACTIVE",
                Map.of(), "operator-1", now(), now());
    }

    private ProgrammaticDspSnapshotView snapshotView() {
        return new ProgrammaticDspSnapshotView(50L, 7L, 10L, 20L, 30L, date(), 1000L, 200L,
                500L, 25L, 5L, 400L, new BigDecimal("250.0000"), new BigDecimal("900.0000"),
                Map.of(), "operator-1", now(), now());
    }

    private ProgrammaticDspSummaryView summaryView() {
        return new ProgrammaticDspSummaryView(7L, 10L, 20L, 30L, date(), date().plusDays(1),
                2, 1500L, 300L, 1000L, 50L, 10L, 750L, new BigDecimal("500.0000"),
                new BigDecimal("1500.0000"), new BigDecimal("1000.0000"), new BigDecimal("0.200000"),
                new BigDecimal("0.050000"), new BigDecimal("0.200000"), new BigDecimal("50.000000"),
                new BigDecimal("3.000000"), new BigDecimal("0.750000"), new BigDecimal("0.500000"),
                "ON_TRACK", now());
    }

    private ProgrammaticDspMutationView mutationView(String status) {
        return new ProgrammaticDspMutationView(
                70L,
                7L,
                10L,
                20L,
                30L,
                40L,
                "DV360",
                "li-bid-raise-1",
                "UPDATE_LINE_ITEM_BID",
                "LINE_ITEM",
                "advertisers/1/lineItems/2",
                "ad217473bfbf4b21f28c7ac96b91a983348173c45eca9e78ddf57592be8ee9df",
                "idem-1",
                status,
                "READY".equals(status) ? "APPROVED" : "PENDING",
                true,
                Map.of("bidCpmMicros", 12500000),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                "operator-1",
                "READY".equals(status) ? "operator-1" : null,
                "READY".equals(status) ? now() : null,
                null,
                null,
                now(),
                now());
    }

    private LocalDate date() {
        return LocalDate.of(2026, 6, 6);
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
