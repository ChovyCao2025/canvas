package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.creator.CreatorCampaignCommand;
import org.chovy.canvas.domain.creator.CreatorCampaignView;
import org.chovy.canvas.domain.creator.CreatorCollaborationCommand;
import org.chovy.canvas.domain.creator.CreatorCollaborationService;
import org.chovy.canvas.domain.creator.CreatorCollaborationView;
import org.chovy.canvas.domain.creator.CreatorDeliverableCommand;
import org.chovy.canvas.domain.creator.CreatorDeliverableView;
import org.chovy.canvas.domain.creator.CreatorPerformanceSummaryQuery;
import org.chovy.canvas.domain.creator.CreatorPerformanceSummaryView;
import org.chovy.canvas.domain.creator.CreatorProfileCommand;
import org.chovy.canvas.domain.creator.CreatorProfileView;
import org.chovy.canvas.domain.creator.CreatorProviderMutationApprovalCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationExecuteCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationQuery;
import org.chovy.canvas.domain.creator.CreatorProviderMutationService;
import org.chovy.canvas.domain.creator.CreatorProviderMutationView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreatorCollaborationControllerTest {

    @Test
    void writeEndpointsPassCurrentTenantAndOperator() {
        CreatorCollaborationService service = mock(CreatorCollaborationService.class);
        CreatorProviderMutationService mutationService = mock(CreatorProviderMutationService.class);
        CreatorProfileCommand creatorCommand = creatorCommand();
        CreatorCampaignCommand campaignCommand = campaignCommand();
        CreatorCollaborationCommand collaborationCommand = collaborationCommand();
        CreatorDeliverableCommand deliverableCommand = deliverableCommand();
        when(service.upsertCreator(7L, creatorCommand, "operator-1")).thenReturn(creatorView());
        when(service.upsertCampaign(7L, campaignCommand, "operator-1")).thenReturn(campaignView());
        when(service.upsertCollaboration(7L, collaborationCommand, "operator-1")).thenReturn(collaborationView());
        when(service.upsertDeliverable(7L, deliverableCommand, "operator-1")).thenReturn(deliverableView());
        CreatorCollaborationController controller = new CreatorCollaborationController(service, mutationService, resolver());

        StepVerifier.create(controller.upsertCreator(creatorCommand))
                .assertNext(response -> assertThat(response.getData().handleKey()).isEqualTo("creator.one"))
                .verifyComplete();
        StepVerifier.create(controller.upsertCampaign(campaignCommand))
                .assertNext(response -> assertThat(response.getData().campaignKey()).isEqualTo("summer-drop"))
                .verifyComplete();
        StepVerifier.create(controller.upsertCollaboration(collaborationCommand))
                .assertNext(response -> assertThat(response.getData().trackingLink()).contains("trk.example"))
                .verifyComplete();
        StepVerifier.create(controller.upsertDeliverable(deliverableCommand))
                .assertNext(response -> assertThat(response.getData().deliverableKey()).isEqualTo("video-1"))
                .verifyComplete();

        verify(service).upsertCreator(7L, creatorCommand, "operator-1");
        verify(service).upsertCampaign(7L, campaignCommand, "operator-1");
        verify(service).upsertCollaboration(7L, collaborationCommand, "operator-1");
        verify(service).upsertDeliverable(7L, deliverableCommand, "operator-1");
    }

    @Test
    void summaryEndpointPassesFilters() {
        CreatorCollaborationService service = mock(CreatorCollaborationService.class);
        CreatorProviderMutationService mutationService = mock(CreatorProviderMutationService.class);
        CreatorPerformanceSummaryQuery query = new CreatorPerformanceSummaryQuery(20L, 10L, 30L, now());
        when(service.summary(7L, query)).thenReturn(summaryView());
        CreatorCollaborationController controller = new CreatorCollaborationController(service, mutationService, resolver());

        StepVerifier.create(controller.summary(20L, 10L, 30L, now()))
                .assertNext(response -> assertThat(response.getData().roi()).isEqualByComparingTo("0.666667"))
                .verifyComplete();

        verify(service).summary(7L, query);
    }

    @Test
    void mutationEndpointsPassCurrentTenantAndOperator() {
        CreatorCollaborationService service = mock(CreatorCollaborationService.class);
        CreatorProviderMutationService mutationService = mock(CreatorProviderMutationService.class);
        CreatorProviderMutationCommand proposeCommand = mutationCommand();
        CreatorProviderMutationApprovalCommand approvalCommand =
                new CreatorProviderMutationApprovalCommand("APPROVED", "ready");
        CreatorProviderMutationExecuteCommand executeCommand =
                new CreatorProviderMutationExecuteCommand(true, true, Map.of("validateOnly", true));
        CreatorProviderMutationQuery query = new CreatorProviderMutationQuery(20L, 30L, "READY", "APPROVED", 20);
        when(mutationService.propose(7L, proposeCommand, "operator-1")).thenReturn(mutationView("DRAFT"));
        when(mutationService.approve(7L, 60L, approvalCommand, "operator-1")).thenReturn(mutationView("READY"));
        when(mutationService.execute(7L, 60L, executeCommand, "operator-1")).thenReturn(mutationView("DRY_RUN_OK"));
        when(mutationService.list(7L, query)).thenReturn(List.of(mutationView("READY")));
        CreatorCollaborationController controller = new CreatorCollaborationController(service, mutationService, resolver());

        StepVerifier.create(controller.proposeMutation(proposeCommand))
                .assertNext(response -> assertThat(response.getData().mutationKey()).isEqualTo("auth-req-1"))
                .verifyComplete();
        StepVerifier.create(controller.approveMutation(60L, approvalCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("READY"))
                .verifyComplete();
        StepVerifier.create(controller.executeMutation(60L, executeCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRY_RUN_OK"))
                .verifyComplete();
        StepVerifier.create(controller.listMutations(20L, 30L, "READY", "APPROVED", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.approvalStatus()).isEqualTo("APPROVED")))
                .verifyComplete();

        verify(mutationService).propose(7L, proposeCommand, "operator-1");
        verify(mutationService).approve(7L, 60L, approvalCommand, "operator-1");
        verify(mutationService).execute(7L, 60L, executeCommand, "operator-1");
        verify(mutationService).list(7L, query);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private CreatorProfileCommand creatorCommand() {
        return new CreatorProfileCommand(
                "TIKTOK",
                "@creator.one",
                "Creator One",
                "KOC",
                "VIDEO",
                120000L,
                new BigDecimal("0.052300"),
                List.of("beauty"),
                "ACTIVE",
                "NORMAL",
                Map.of());
    }

    private CreatorCampaignCommand campaignCommand() {
        return new CreatorCampaignCommand(
                "summer-drop",
                "Summer Drop",
                "CONVERSION",
                new BigDecimal("50000.0000"),
                "USD",
                now().minusDays(1),
                now().plusDays(30),
                "ACTIVE",
                Map.of());
    }

    private CreatorCollaborationCommand collaborationCommand() {
        return new CreatorCollaborationCommand(
                20L,
                10L,
                "AFFILIATE",
                new BigDecimal("100.0000"),
                new BigDecimal("0.100000"),
                "https://trk.example/c1",
                "SUMMER10",
                "ACTIVE",
                Map.of(),
                Map.of());
    }

    private CreatorDeliverableCommand deliverableCommand() {
        return new CreatorDeliverableCommand(
                30L,
                "video-1",
                "VIDEO",
                "TIKTOK",
                now().plusDays(7),
                null,
                null,
                "PLANNED",
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                BigDecimal.ZERO,
                Map.of());
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

    private CreatorProfileView creatorView() {
        return new CreatorProfileView(10L, 7L, "TIKTOK", "@creator.one", "creator.one", "Creator One",
                "KOC", "VIDEO", 120000L, new BigDecimal("0.052300"), List.of("beauty"),
                "ACTIVE", "NORMAL", Map.of(), "operator-1", now(), now());
    }

    private CreatorCampaignView campaignView() {
        return new CreatorCampaignView(20L, 7L, "summer-drop", "Summer Drop", "CONVERSION",
                new BigDecimal("50000.0000"), "USD", now().minusDays(1), now().plusDays(30),
                "ACTIVE", Map.of(), "operator-1", now(), now());
    }

    private CreatorCollaborationView collaborationView() {
        return new CreatorCollaborationView(30L, 7L, 20L, 10L, "AFFILIATE",
                new BigDecimal("100.0000"), new BigDecimal("0.100000"), "https://trk.example/c1",
                "SUMMER10", "ACTIVE", Map.of(), Map.of(), "operator-1", now(), now());
    }

    private CreatorDeliverableView deliverableView() {
        return new CreatorDeliverableView(40L, 7L, 30L, 20L, 10L, "video-1", "VIDEO", "TIKTOK",
                now().plusDays(7), null, null, "PLANNED", 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                BigDecimal.ZERO, Map.of(), "operator-1", now(), now());
    }

    private CreatorPerformanceSummaryView summaryView() {
        return new CreatorPerformanceSummaryView(7L, 20L, 10L, 30L, 2, 1, 1,
                1500L, 22L, 25L, 2L, new BigDecimal("200.0000"), new BigDecimal("100.0000"),
                new BigDecimal("20.0000"), new BigDecimal("120.0000"), new BigDecimal("0.666667"), now());
    }

    private CreatorProviderMutationView mutationView(String status) {
        return new CreatorProviderMutationView(
                60L,
                7L,
                20L,
                30L,
                40L,
                10L,
                "TIKTOK",
                "auth-req-1",
                "REQUEST_CONTENT_AUTHORIZATION",
                "DELIVERABLE",
                "video-remote-1",
                "hash",
                "idem-1",
                status,
                "READY".equals(status) || "DRY_RUN_OK".equals(status) ? "APPROVED" : "PENDING",
                true,
                Map.of("sparkAuthorizationCode", "AUTH-123"),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                "operator-1",
                "operator-1",
                now(),
                "operator-1",
                now(),
                now(),
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
