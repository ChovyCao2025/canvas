package org.chovy.canvas.domain.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.CreatorCollaborationDO;
import org.chovy.canvas.dal.dataobject.CreatorDeliverableDO;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;
import org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorCollaborationMapper;
import org.chovy.canvas.dal.mapper.CreatorDeliverableMapper;
import org.chovy.canvas.dal.mapper.CreatorProfileMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
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

class CreatorProviderMutationServiceTest {

    @Test
    void proposesCreatorProviderMutationWithTenantOwnedRelationship() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable());
        when(profileMapper.selectById(10L)).thenReturn(creator());
        doAnswer(invocation -> {
            invocation.<CreatorProviderMutationDO>getArgument(0).setId(60L);
            return 1;
        }).when(mutationMapper).insert(any(CreatorProviderMutationDO.class));
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, CreatorProviderWriteGateway.unsupported());

        CreatorProviderMutationView view = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(view.id()).isEqualTo(60L);
        assertThat(view.provider()).isEqualTo("TIKTOK");
        assertThat(view.mutationType()).isEqualTo("REQUEST_CONTENT_AUTHORIZATION");
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.approvalStatus()).isEqualTo("PENDING");
        assertThat(view.requestHash()).hasSize(64);
        assertThat(view.payload()).containsEntry("sparkAuthorizationCode", "AUTH-123");
        ArgumentCaptor<CreatorProviderMutationDO> captor =
                ArgumentCaptor.forClass(CreatorProviderMutationDO.class);
        verify(mutationMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getCampaignId()).isEqualTo(20L);
        assertThat(captor.getValue().getCollaborationId()).isEqualTo(30L);
        assertThat(captor.getValue().getDeliverableId()).isEqualTo(40L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void rejectsMutationWhenDeliverableDoesNotBelongToCollaboration() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        CreatorDeliverableDO deliverable = deliverable();
        deliverable.setCollaborationId(999L);
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable);
        when(profileMapper.selectById(10L)).thenReturn(creator());
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, CreatorProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L, mutationCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deliverable");

        verify(mutationMapper, never()).insert(any(CreatorProviderMutationDO.class));
    }

    @Test
    void rejectsNestedProviderSecretsInPayload() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable());
        when(profileMapper.selectById(10L)).thenReturn(creator());
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, CreatorProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L,
                new CreatorProviderMutationCommand(20L, 30L, 40L, "auth-req-secret",
                        "REQUEST_CONTENT_AUTHORIZATION", "DELIVERABLE", "video-remote-1",
                        true, "idem-secret", Map.of(
                        "sparkAuthorizationCode", "AUTH-123",
                        "providerAuth", Map.of("client_secret", "secret-value"))), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider secrets");

        verify(mutationMapper, never()).insert(any(CreatorProviderMutationDO.class));
    }

    @Test
    void keepsMutationIdempotentByMutationKeyAndRequestHash() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable());
        when(profileMapper.selectById(10L)).thenReturn(creator());
        when(mutationMapper.selectOne(any())).thenReturn(mutation());
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, CreatorProviderWriteGateway.unsupported());

        CreatorProviderMutationView idempotent = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(idempotent.id()).isEqualTo(60L);
        verify(mutationMapper, never()).insert(any(CreatorProviderMutationDO.class));
        assertThatThrownBy(() -> service.propose(7L,
                new CreatorProviderMutationCommand(20L, 30L, 40L, "auth-req-1",
                        "REQUEST_CONTENT_AUTHORIZATION", "DELIVERABLE", "video-remote-1",
                        true, "idem-1", Map.of("sparkAuthorizationCode", "DIFFERENT")), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request hash");
    }

    @Test
    void enforcesApprovalAndDryRunBeforeApplyAndFailsClosedWhenLiveClientUnavailable() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        CreatorProviderMutationDO row = mutation();
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable());
        when(profileMapper.selectById(10L)).thenReturn(creator());
        when(mutationMapper.selectById(60L)).thenReturn(row);
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, CreatorProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.execute(7L, 60L,
                new CreatorProviderMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");

        CreatorProviderMutationView approved = service.approve(7L, 60L,
                new CreatorProviderMutationApprovalCommand("APPROVED", "ready"), "lead-1");
        assertThat(approved.status()).isEqualTo("READY");
        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> service.execute(7L, 60L,
                new CreatorProviderMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dry run");

        CreatorProviderMutationView dryRun = service.execute(7L, 60L,
                new CreatorProviderMutationExecuteCommand(true, true, Map.of("validateOnly", true)), "operator-1");
        assertThat(dryRun.status()).isEqualTo("DRY_RUN_OK");

        CreatorProviderMutationView live = service.execute(7L, 60L,
                new CreatorProviderMutationExecuteCommand(false, true, Map.of()), "operator-1");
        assertThat(live.status()).isEqualTo("FAILED");
        assertThat(live.errorCode()).isEqualTo("PROVIDER_CLIENT_UNAVAILABLE");
    }

    @Test
    void redactsProviderRequestMetadataAndResponseEvidence() {
        CreatorCampaignMapper campaignMapper = mock(CreatorCampaignMapper.class);
        CreatorCollaborationMapper collaborationMapper = mock(CreatorCollaborationMapper.class);
        CreatorDeliverableMapper deliverableMapper = mock(CreatorDeliverableMapper.class);
        CreatorProfileMapper profileMapper = mock(CreatorProfileMapper.class);
        CreatorProviderMutationMapper mutationMapper = mock(CreatorProviderMutationMapper.class);
        CreatorProviderMutationDO row = mutation();
        row.setStatus("READY");
        row.setApprovalStatus("APPROVED");
        when(campaignMapper.selectById(20L)).thenReturn(campaign());
        when(collaborationMapper.selectById(30L)).thenReturn(collaboration());
        when(deliverableMapper.selectById(40L)).thenReturn(deliverable());
        when(profileMapper.selectById(10L)).thenReturn(creator());
        when(mutationMapper.selectById(60L)).thenReturn(row);
        CreatorProviderWriteClient client = new CreatorProviderWriteClient() {
            @Override
            public boolean supports(CreatorProviderMutationRequest request) {
                return true;
            }

            @Override
            public CreatorProviderMutationResult execute(CreatorProviderMutationRequest request) {
                return CreatorProviderMutationResult.success("creator-validate-1", Map.of(
                        "requestId", "creator-request-1",
                        "api_key", "raw-key",
                        "nested", Map.of("client_secret", "raw-secret", "status", "OK")));
            }
        };
        CreatorProviderMutationService service = service(campaignMapper, collaborationMapper, deliverableMapper,
                profileMapper, mutationMapper, new CreatorProviderWriteGateway(List.of(client)));

        CreatorProviderMutationView view = service.execute(7L, 60L,
                new CreatorProviderMutationExecuteCommand(true, true, Map.of(
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
                .containsEntry("api_key", org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED)
                .containsEntry("requestId", "creator-request-1");
        Map<?, ?> responseNested = (Map<?, ?>) view.providerResponse().get("nested");
        assertThat(responseNested.get("client_secret"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(responseNested.get("status")).isEqualTo("OK");
    }

    private CreatorProviderMutationService service(CreatorCampaignMapper campaignMapper,
                                                   CreatorCollaborationMapper collaborationMapper,
                                                   CreatorDeliverableMapper deliverableMapper,
                                                   CreatorProfileMapper profileMapper,
                                                   CreatorProviderMutationMapper mutationMapper,
                                                   CreatorProviderWriteGateway gateway) {
        return new CreatorProviderMutationService(
                campaignMapper,
                collaborationMapper,
                deliverableMapper,
                profileMapper,
                mutationMapper,
                new ObjectMapper(),
                gateway,
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private CreatorProviderMutationCommand mutationCommand() {
        return new CreatorProviderMutationCommand(
                20L,
                30L,
                40L,
                "auth-req-1",
                "REQUEST_CONTENT_AUTHORIZATION",
                "DELIVERABLE",
                "video-remote-1",
                true,
                "idem-1",
                Map.of("sparkAuthorizationCode", "AUTH-123"));
    }

    private CreatorProviderMutationDO mutation() {
        CreatorProviderMutationDO row = new CreatorProviderMutationDO();
        row.setId(60L);
        row.setTenantId(7L);
        row.setCampaignId(20L);
        row.setCollaborationId(30L);
        row.setDeliverableId(40L);
        row.setCreatorId(10L);
        row.setProvider("TIKTOK");
        row.setMutationKey("auth-req-1");
        row.setMutationType("REQUEST_CONTENT_AUTHORIZATION");
        row.setEntityType("DELIVERABLE");
        row.setExternalEntityId("video-remote-1");
        row.setRequestHash("6d7633a021f7d6cc273318c38f326fead2f8434e847c2f08ab6d9c210b54dacc");
        row.setIdempotencyKey("idem-1");
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        row.setDryRunRequired(1);
        row.setPayloadJson("{\"sparkAuthorizationCode\":\"AUTH-123\"}");
        row.setValidationJson("{}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private CreatorCampaignDO campaign() {
        CreatorCampaignDO row = new CreatorCampaignDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setCampaignKey("summer-drop");
        row.setCampaignName("Summer Drop");
        row.setStatus("ACTIVE");
        return row;
    }

    private CreatorCollaborationDO collaboration() {
        CreatorCollaborationDO row = new CreatorCollaborationDO();
        row.setId(30L);
        row.setTenantId(7L);
        row.setCampaignId(20L);
        row.setCreatorId(10L);
        row.setStatus("ACTIVE");
        return row;
    }

    private CreatorDeliverableDO deliverable() {
        CreatorDeliverableDO row = new CreatorDeliverableDO();
        row.setId(40L);
        row.setTenantId(7L);
        row.setCollaborationId(30L);
        row.setCampaignId(20L);
        row.setCreatorId(10L);
        row.setDeliverableKey("video-1");
        row.setContentType("VIDEO");
        row.setPlatform("TIKTOK");
        row.setStatus("PLANNED");
        return row;
    }

    private CreatorProfileDO creator() {
        CreatorProfileDO row = new CreatorProfileDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("TIKTOK");
        row.setHandle("@creator.one");
        row.setHandleKey("creator.one");
        row.setStatus("ACTIVE");
        row.setRiskStatus("NORMAL");
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
