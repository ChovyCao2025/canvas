package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.ProgrammaticDspFacade;
import org.junit.jupiter.api.Test;

class ProgrammaticDspApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void managesProgrammaticDspCatalogAndSummaryWithinTenant() {
        ProgrammaticDspFacade service = new ProgrammaticDspApplicationService(CLOCK);

        Map<String, Object> seat = service.upsertSeat(7L, Map.of("seatKey", "seat-a", "name", "Seat A"),
                "operator-1");
        Map<String, Object> campaign = service.upsertCampaign(7L, Map.of(
                "campaignKey", "cmp-a",
                "seatId", 1,
                "name", "Launch campaign",
                "budget", 1200), "operator-1");
        Map<String, Object> lineItem = service.upsertLineItem(7L, Map.of(
                "lineItemKey", "li-a",
                "campaignId", 1,
                "dailyBudget", 200,
                "bidCpm", 12.5), "operator-2");
        Map<String, Object> supplyPath = service.upsertSupplyPath(7L, Map.of(
                "supplyPathKey", "sp-a",
                "seatId", 1,
                "exchange", "openx",
                "priority", 4), "operator-2");
        Map<String, Object> snapshot = service.recordSnapshot(7L, Map.of(
                "lineItemId", 1,
                "impressions", 1000,
                "clicks", 25,
                "spend", 125.75), "operator-3");
        Map<String, Object> summary = service.summary(7L, Map.of("seatId", 1));

        assertThat(seat).containsEntry("tenantId", 7L)
                .containsEntry("id", 1L)
                .containsEntry("seatKey", "seat-a")
                .containsEntry("updatedBy", "operator-1");
        assertThat(campaign).containsEntry("id", 1L)
                .containsEntry("seatId", 1L)
                .containsEntry("status", "ACTIVE")
                .containsEntry("budget", 1200.0);
        assertThat(lineItem).containsEntry("campaignId", 1L)
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-2");
        assertThat(supplyPath).containsEntry("exchange", "openx")
                .containsEntry("priority", 4);
        assertThat(snapshot).containsEntry("lineItemId", 1L)
                .containsEntry("impressions", 1000L)
                .containsEntry("clicks", 25L)
                .containsEntry("spend", 125.75);
        assertThat(summary).containsEntry("tenantId", 7L)
                .containsEntry("seatCount", 1)
                .containsEntry("campaignCount", 1)
                .containsEntry("lineItemCount", 1)
                .containsEntry("supplyPathCount", 1)
                .containsEntry("snapshotCount", 1)
                .containsEntry("impressions", 1000L)
                .containsEntry("clicks", 25L)
                .containsEntry("spend", 125.75);
        assertThat(service.summary(8L, Map.of())).containsEntry("seatCount", 0);
    }

    @Test
    void mutationLifecycleIsDeterministicAndScoped() {
        ProgrammaticDspFacade service = new ProgrammaticDspApplicationService(CLOCK);
        service.upsertSeat(7L, Map.of("seatKey", "seat-a"), "operator-1");
        service.upsertCampaign(7L, Map.of("campaignKey", "cmp-a", "seatId", 1), "operator-1");
        service.upsertLineItem(7L, Map.of("lineItemKey", "li-a", "campaignId", 1), "operator-1");

        Map<String, Object> mutation = service.proposeMutation(7L, Map.of(
                "lineItemId", 1,
                "mutationType", "BID_UPDATE",
                "dryRun", true,
                "payload", Map.of("bidCpm", 14.0)), "planner");
        Map<String, Object> approved = service.approveMutation(7L, 1L, Map.of("comment", "approved"),
                "approver");
        Map<String, Object> executed = service.executeMutation(7L, 1L, Map.of("providerRequestId", "req-1"),
                "executor");
        List<Map<String, Object>> listed = service.listMutations(7L, Map.of("status", "EXECUTED", "limit", 10));

        assertThat(mutation).containsEntry("id", 1L)
                .containsEntry("tenantId", 7L)
                .containsEntry("lineItemId", 1L)
                .containsEntry("mutationType", "BID_UPDATE")
                .containsEntry("status", "PROPOSED")
                .containsEntry("approvalStatus", "PENDING")
                .containsEntry("createdBy", "planner");
        assertThat(approved).containsEntry("status", "APPROVED")
                .containsEntry("approvalStatus", "APPROVED")
                .containsEntry("approvedBy", "approver");
        assertThat(executed).containsEntry("status", "EXECUTED")
                .containsEntry("executedBy", "executor")
                .containsEntry("providerRequestId", "req-1");
        assertThat(listed).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));
        assertThat(service.listMutations(8L, Map.of("limit", 10))).isEmpty();
    }

    @Test
    void defaultsValidationAndQueryNormalizationFollowCompatibilityRules() {
        ProgrammaticDspFacade service = new ProgrammaticDspApplicationService(CLOCK);

        Map<String, Object> seat = service.upsertSeat(null, Map.of(), "");
        Map<String, Object> summary = service.summary(null, Map.of(
                "startDate", LocalDate.parse("2026-06-01"),
                "endDate", LocalDate.parse("2026-06-30"),
                "evaluatedAt", LocalDateTime.parse("2026-06-14T11:00:00")));

        assertThat(seat).containsEntry("tenantId", 0L)
                .containsEntry("seatKey", "seat-1")
                .containsEntry("updatedBy", "system");
        assertThat(summary).containsEntry("tenantId", 0L)
                .containsEntry("startDate", LocalDate.parse("2026-06-01"))
                .containsEntry("endDate", LocalDate.parse("2026-06-30"))
                .containsEntry("evaluatedAt", LocalDateTime.parse("2026-06-14T11:00:00"));

        assertThatThrownBy(() -> service.upsertCampaign(7L, Map.of("seatId", 99), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DSP seat not found");
        assertThatThrownBy(() -> service.approveMutation(7L, 99L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DSP mutation not found");
        assertThatThrownBy(() -> service.summary(7L, Map.of("startDate", "bad-date")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date");
    }
}
