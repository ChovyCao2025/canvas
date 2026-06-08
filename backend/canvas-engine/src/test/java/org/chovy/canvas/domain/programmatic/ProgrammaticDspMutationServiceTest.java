package org.chovy.canvas.domain.programmatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO;
import org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgrammaticDspMutationServiceTest {

    @Test
    void proposesDspMutationWithTenantOwnedSeatCampaignLineItemAndSupplyPath() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem());
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        doAnswer(invocation -> {
            invocation.<ProgrammaticDspMutationDO>getArgument(0).setId(70L);
            return 1;
        }).when(mutationMapper).insert(any(ProgrammaticDspMutationDO.class));
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, ProgrammaticDspProviderWriteGateway.unsupported());

        ProgrammaticDspMutationView view = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(view.id()).isEqualTo(70L);
        assertThat(view.provider()).isEqualTo("DV360");
        assertThat(view.mutationType()).isEqualTo("UPDATE_LINE_ITEM_BID");
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.approvalStatus()).isEqualTo("PENDING");
        assertThat(view.requestHash()).hasSize(64);
        assertThat(view.payload()).containsEntry("bidCpmMicros", 12500000);
        ArgumentCaptor<ProgrammaticDspMutationDO> captor =
                ArgumentCaptor.forClass(ProgrammaticDspMutationDO.class);
        verify(mutationMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getSeatId()).isEqualTo(10L);
        assertThat(captor.getValue().getCampaignId()).isEqualTo(20L);
        assertThat(captor.getValue().getLineItemId()).isEqualTo(30L);
        assertThat(captor.getValue().getSupplyPathId()).isEqualTo(40L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void rejectsMutationWhenLineItemDoesNotBelongToSeat() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        ProgrammaticDspLineItemDO lineItem = lineItem();
        lineItem.setSeatId(999L);
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem);
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, ProgrammaticDspProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L, mutationCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line item");

        verify(mutationMapper, never()).insert(any(ProgrammaticDspMutationDO.class));
    }

    @Test
    void rejectsNestedProviderSecretsInPayload() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem());
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, ProgrammaticDspProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L,
                new ProgrammaticDspMutationCommand(10L, 20L, 30L, 40L, "li-bid-raise-secret",
                        "UPDATE_LINE_ITEM_BID", "LINE_ITEM", "advertisers/1/lineItems/2",
                        true, "idem-secret", Map.of(
                        "bidCpmMicros", 12500000,
                        "providerAuth", Map.of("access_token", "token-value"))), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider secrets");

        verify(mutationMapper, never()).insert(any(ProgrammaticDspMutationDO.class));
    }

    @Test
    void keepsMutationIdempotentByMutationKeyAndRequestHash() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem());
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        when(mutationMapper.selectOne(any())).thenReturn(mutation());
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, ProgrammaticDspProviderWriteGateway.unsupported());

        ProgrammaticDspMutationView idempotent = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(idempotent.id()).isEqualTo(70L);
        verify(mutationMapper, never()).insert(any(ProgrammaticDspMutationDO.class));
        assertThatThrownBy(() -> service.propose(7L,
                new ProgrammaticDspMutationCommand(10L, 20L, 30L, 40L, "li-bid-raise-1",
                        "UPDATE_LINE_ITEM_BID", "LINE_ITEM", "advertisers/1/lineItems/2",
                        true, "idem-1", Map.of("bidCpmMicros", 15000000)), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request hash");
    }

    @Test
    void enforcesApprovalAndDryRunBeforeApplyAndFailsClosedWhenLiveClientUnavailable() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        ProgrammaticDspMutationDO row = mutation();
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem());
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        when(mutationMapper.selectById(70L)).thenReturn(row);
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, ProgrammaticDspProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.execute(7L, 70L,
                new ProgrammaticDspMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");

        ProgrammaticDspMutationView approved = service.approve(7L, 70L,
                new ProgrammaticDspMutationApprovalCommand("APPROVED", "ready"), "lead-1");
        assertThat(approved.status()).isEqualTo("READY");
        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> service.execute(7L, 70L,
                new ProgrammaticDspMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dry run");

        ProgrammaticDspMutationView dryRun = service.execute(7L, 70L,
                new ProgrammaticDspMutationExecuteCommand(true, true, Map.of("validateOnly", true)), "operator-1");
        assertThat(dryRun.status()).isEqualTo("DRY_RUN_OK");

        ProgrammaticDspMutationView live = service.execute(7L, 70L,
                new ProgrammaticDspMutationExecuteCommand(false, true, Map.of()), "operator-1");
        assertThat(live.status()).isEqualTo("FAILED");
        assertThat(live.errorCode()).isEqualTo("PROVIDER_CLIENT_UNAVAILABLE");
    }

    @Test
    void rejectsSelfApprovalForLiveProgrammaticDspProviderMutation() {
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        ProgrammaticDspMutationDO row = mutation();
        row.setDryRunRequired(1);
        when(mutationMapper.selectById(70L)).thenReturn(row);
        ProgrammaticDspMutationService service = service(mock(ProgrammaticDspSeatMapper.class),
                mock(ProgrammaticDspCampaignMapper.class), mock(ProgrammaticDspLineItemMapper.class),
                mock(ProgrammaticDspSupplyPathMapper.class), mutationMapper,
                ProgrammaticDspProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.approve(7L, 70L,
                new ProgrammaticDspMutationApprovalCommand("APPROVED", "self-review"), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("creator cannot approve");

        verify(mutationMapper, never()).updateById(any(ProgrammaticDspMutationDO.class));
    }

    @Test
    void redactsProviderRequestMetadataAndResponseEvidence() {
        ProgrammaticDspSeatMapper seatMapper = mock(ProgrammaticDspSeatMapper.class);
        ProgrammaticDspCampaignMapper campaignMapper = mock(ProgrammaticDspCampaignMapper.class);
        ProgrammaticDspLineItemMapper lineItemMapper = mock(ProgrammaticDspLineItemMapper.class);
        ProgrammaticDspSupplyPathMapper supplyPathMapper = mock(ProgrammaticDspSupplyPathMapper.class);
        ProgrammaticDspMutationMapper mutationMapper = mock(ProgrammaticDspMutationMapper.class);
        ProgrammaticDspMutationDO row = mutation();
        row.setStatus("READY");
        row.setApprovalStatus("APPROVED");
        when(seatMapper.selectById(10L)).thenReturn(seat());
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(lineItemMapper.selectById(30L)).thenReturn(lineItem());
        when(supplyPathMapper.selectById(40L)).thenReturn(supplyPath());
        when(mutationMapper.selectById(70L)).thenReturn(row);
        ProgrammaticDspProviderWriteClient client = new ProgrammaticDspProviderWriteClient() {
            @Override
            public boolean supports(ProgrammaticDspMutationRequest request) {
                return true;
            }

            @Override
            public ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request) {
                return ProgrammaticDspMutationResult.success("dsp-validate-1", Map.of(
                        "requestId", "dsp-request-1",
                        "secret", "raw-secret",
                        "nested", Map.of("access_token", "raw-token", "status", "OK")));
            }
        };
        ProgrammaticDspMutationService service = service(seatMapper, campaignMapper, lineItemMapper,
                supplyPathMapper, mutationMapper, new ProgrammaticDspProviderWriteGateway(List.of(client)));

        ProgrammaticDspMutationView view = service.execute(7L, 70L,
                new ProgrammaticDspMutationExecuteCommand(true, true, Map.of(
                        "authorization", "Bearer raw",
                        "nested", Map.of("refresh_token", "raw-refresh", "note", "safe"))),
                "operator-1");

        Map<?, ?> metadata = (Map<?, ?>) view.providerRequest().get("metadata");
        assertThat(metadata.get("authorization"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        Map<?, ?> requestNested = (Map<?, ?>) metadata.get("nested");
        assertThat(requestNested.get("refresh_token"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(requestNested.get("note")).isEqualTo("safe");
        assertThat(view.providerResponse())
                .containsEntry("secret", org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED)
                .containsEntry("requestId", "dsp-request-1");
        Map<?, ?> responseNested = (Map<?, ?>) view.providerResponse().get("nested");
        assertThat(responseNested.get("access_token"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(responseNested.get("status")).isEqualTo("OK");
    }

    private ProgrammaticDspMutationService service(ProgrammaticDspSeatMapper seatMapper,
                                                   ProgrammaticDspCampaignMapper campaignMapper,
                                                   ProgrammaticDspLineItemMapper lineItemMapper,
                                                   ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                                   ProgrammaticDspMutationMapper mutationMapper,
                                                   ProgrammaticDspProviderWriteGateway gateway) {
        return new ProgrammaticDspMutationService(
                seatMapper,
                campaignMapper,
                lineItemMapper,
                supplyPathMapper,
                mutationMapper,
                new ObjectMapper(),
                gateway,
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
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

    private ProgrammaticDspMutationDO mutation() {
        ProgrammaticDspMutationDO row = new ProgrammaticDspMutationDO();
        row.setId(70L);
        row.setTenantId(7L);
        row.setSeatId(10L);
        row.setCampaignId(20L);
        row.setLineItemId(30L);
        row.setSupplyPathId(40L);
        row.setProvider("DV360");
        row.setMutationKey("li-bid-raise-1");
        row.setMutationType("UPDATE_LINE_ITEM_BID");
        row.setEntityType("LINE_ITEM");
        row.setExternalEntityId("advertisers/1/lineItems/2");
        row.setRequestHash("ad217473bfbf4b21f28c7ac96b91a983348173c45eca9e78ddf57592be8ee9df");
        row.setIdempotencyKey("idem-1");
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        row.setDryRunRequired(1);
        row.setPayloadJson("{\"bidCpmMicros\":12500000}");
        row.setValidationJson("{}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private ProgrammaticDspSeatDO seat() {
        ProgrammaticDspSeatDO row = new ProgrammaticDspSeatDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("DV360");
        row.setSeatKey("seat-main");
        row.setDisplayName("Main Seat");
        row.setAdvertiserAccountId("advertisers/1");
        row.setCurrency("USD");
        row.setTimezone("UTC");
        row.setSupplyChainEnforcement("ENFORCE");
        row.setEnabled(1);
        return row;
    }

    private ProgrammaticDspCampaignDO campaign() {
        ProgrammaticDspCampaignDO row = new ProgrammaticDspCampaignDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setCampaignKey("summer-2026");
        row.setCampaignName("Summer 2026");
        row.setObjective("AWARENESS");
        row.setBudgetAmount(new BigDecimal("5000.0000"));
        row.setCurrency("USD");
        row.setStatus("ACTIVE");
        return row;
    }

    private ProgrammaticDspLineItemDO lineItem() {
        ProgrammaticDspLineItemDO row = new ProgrammaticDspLineItemDO();
        row.setId(30L);
        row.setTenantId(7L);
        row.setSeatId(10L);
        row.setCampaignId(20L);
        row.setLineItemKey("li-main");
        row.setLineItemName("Main Line Item");
        row.setBidStrategy("AUTO");
        row.setMaxBidCpm(new BigDecimal("12.5000"));
        row.setDailyBudgetAmount(new BigDecimal("100.0000"));
        row.setTotalBudgetAmount(new BigDecimal("1000.0000"));
        row.setPacingMode("EVEN");
        row.setTargetingJson("{\"geo\":\"US\"}");
        row.setFrequencyCap(3);
        row.setStatus("ACTIVE");
        return row;
    }

    private ProgrammaticDspSupplyPathDO supplyPath() {
        ProgrammaticDspSupplyPathDO row = new ProgrammaticDspSupplyPathDO();
        row.setId(40L);
        row.setTenantId(7L);
        row.setLineItemId(30L);
        row.setExchangeKey("OPENX");
        row.setDealId("deal-1");
        row.setSellerId("seller-9");
        row.setSellerDomain("publisher.example");
        row.setInventoryType("VIDEO");
        row.setAdsTxtStatus("AUTHORIZED");
        row.setSellersJsonStatus("MATCHED");
        row.setSchainComplete(1);
        row.setStatus("ACTIVE");
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
