package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAiReplyGenerateCommand;
import org.chovy.canvas.domain.conversation.ConversationAiReplyReviewCommand;
import org.chovy.canvas.domain.conversation.ConversationAiReplyService;
import org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionQuery;
import org.chovy.canvas.domain.conversation.ConversationAiReplySuggestionView;
import org.chovy.canvas.domain.conversation.ConversationAssignmentCommand;
import org.chovy.canvas.domain.conversation.ConversationContactProfileView;
import org.chovy.canvas.domain.conversation.ConversationInboxQuery;
import org.chovy.canvas.domain.conversation.ConversationRouteCommand;
import org.chovy.canvas.domain.conversation.ConversationRouteResultView;
import org.chovy.canvas.domain.conversation.ConversationRoutingAgentCommand;
import org.chovy.canvas.domain.conversation.ConversationRoutingAgentView;
import org.chovy.canvas.domain.conversation.ConversationRoutingRuleCommand;
import org.chovy.canvas.domain.conversation.ConversationRoutingRuleView;
import org.chovy.canvas.domain.conversation.ConversationRoutingService;
import org.chovy.canvas.domain.conversation.ConversationSlaBreachView;
import org.chovy.canvas.domain.conversation.ConversationSlaEvaluationView;
import org.chovy.canvas.domain.conversation.ConversationSopTaskCommand;
import org.chovy.canvas.domain.conversation.ConversationSopTaskCompletionCommand;
import org.chovy.canvas.domain.conversation.ConversationSopTaskView;
import org.chovy.canvas.domain.conversation.ConversationWorkItemAuditView;
import org.chovy.canvas.domain.conversation.ConversationWorkItemStatusCommand;
import org.chovy.canvas.domain.conversation.ConversationWorkItemView;
import org.chovy.canvas.domain.conversation.ConversationWorkspaceService;
import org.chovy.canvas.domain.conversation.ConversationWorkspaceTimelineView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationWorkspaceControllerTest {

    @Test
    void ensureWorkItemUsesCurrentTenantAndOperator() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        when(service.ensureWorkItemForSession(7L, 100L, "operator-1")).thenReturn(workItem());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.ensureWorkItem(100L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().id()).isEqualTo(400L);
                })
                .verifyComplete();

        verify(service).ensureWorkItemForSession(7L, 100L, "operator-1");
    }

    @Test
    void inboxPassesFiltersAndBoundedLimit() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        when(service.inbox(7L, new ConversationInboxQuery("OPEN", "alice", "WEB_CHAT", 100)))
                .thenReturn(List.of(workItem()));
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.inbox("OPEN", "alice", "WEB_CHAT", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.assignedTo()).isEqualTo("alice")))
                .verifyComplete();

        verify(service).inbox(7L, new ConversationInboxQuery("OPEN", "alice", "WEB_CHAT", 100));
    }

    @Test
    void assignmentEndpointPassesActor() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationAssignmentCommand command = new ConversationAssignmentCommand("alice", "sales", "VIP");
        when(service.assign(7L, 400L, command, "operator-1")).thenReturn(workItem());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.assign(400L, command))
                .assertNext(response -> assertThat(response.getData().assignedTo()).isEqualTo("alice"))
                .verifyComplete();

        verify(service).assign(7L, 400L, command, "operator-1");
    }

    @Test
    void statusEndpointPassesActor() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationWorkItemStatusCommand command =
                new ConversationWorkItemStatusCommand("SNOOZED", "HIGH", now().plusDays(1), "follow up");
        when(service.updateStatus(7L, 400L, command, "operator-1")).thenReturn(workItem());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.updateStatus(400L, command))
                .assertNext(response -> assertThat(response.getData().id()).isEqualTo(400L))
                .verifyComplete();

        verify(service).updateStatus(7L, 400L, command, "operator-1");
    }

    @Test
    void taskEndpointsPassActor() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationSopTaskCommand create = new ConversationSopTaskCommand(
                "book_demo",
                "Book a demo",
                "alice",
                now().plusHours(2),
                Map.of("playbook", "sales_handoff"));
        ConversationSopTaskCompletionCommand complete = new ConversationSopTaskCompletionCommand("done");
        when(service.createTask(7L, 400L, create, "operator-1")).thenReturn(task());
        when(service.completeTask(7L, 500L, complete, "operator-1")).thenReturn(task());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.createTask(400L, create))
                .assertNext(response -> assertThat(response.getData().id()).isEqualTo(500L))
                .verifyComplete();
        StepVerifier.create(controller.completeTask(500L, complete))
                .assertNext(response -> assertThat(response.getData().taskKey()).isEqualTo("book_demo"))
                .verifyComplete();

        verify(service).createTask(7L, 400L, create, "operator-1");
        verify(service).completeTask(7L, 500L, complete, "operator-1");
    }

    @Test
    void timelineEndpointUsesTenantAndBoundedLimits() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationWorkspaceTimelineView timeline = new ConversationWorkspaceTimelineView(
                workItem(),
                new ConversationContactProfileView(
                        300L, 7L, "user-1", "user-1", null, "WEB_CHAT", "alice",
                        "NEW", List.of(), Map.of(), now(), now()),
                null,
                List.of(),
                List.of(task()),
                List.of(new ConversationWorkItemAuditView(
                        600L, 7L, 400L, "CREATED", "system", Map.of(), Map.of(), null, now())));
        when(service.timeline(7L, 400L, 100, 1)).thenReturn(timeline);
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), resolver());

        StepVerifier.create(controller.timeline(400L, 200, 0))
                .assertNext(response -> assertThat(response.getData().tasks()).singleElement()
                        .satisfies(task -> assertThat(task.id()).isEqualTo(500L)))
                .verifyComplete();

        verify(service).timeline(7L, 400L, 100, 1);
    }

    @Test
    void routingAgentEndpointPassesTenantOperatorAndCommand() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationRoutingService routingService = mock(ConversationRoutingService.class);
        ConversationRoutingAgentCommand command = agentCommand();
        when(routingService.upsertAgent(7L, command, "operator-1")).thenReturn(agentView());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService, resolver());

        StepVerifier.create(controller.upsertRoutingAgent(command))
                .assertNext(response -> assertThat(response.getData().agentKey()).isEqualTo("alice"))
                .verifyComplete();

        verify(routingService).upsertAgent(7L, command, "operator-1");
    }

    @Test
    void routingRuleEndpointPassesTenantOperatorAndCommand() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationRoutingService routingService = mock(ConversationRoutingService.class);
        ConversationRoutingRuleCommand command = ruleCommand();
        when(routingService.upsertRule(7L, command, "operator-1")).thenReturn(ruleView());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService, resolver());

        StepVerifier.create(controller.upsertRoutingRule(command))
                .assertNext(response -> assertThat(response.getData().ruleKey()).isEqualTo("vip-sales"))
                .verifyComplete();

        verify(routingService).upsertRule(7L, command, "operator-1");
    }

    @Test
    void routeWorkItemEndpointPassesTenantOperatorAndCommand() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationRoutingService routingService = mock(ConversationRoutingService.class);
        ConversationRouteCommand command = new ConversationRouteCommand(
                List.of("sales", "vip"), "sales", 30, "VIP handoff");
        when(routingService.routeWorkItem(7L, 400L, command, "operator-1")).thenReturn(routeView());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService, resolver());

        StepVerifier.create(controller.routeWorkItem(400L, command))
                .assertNext(response -> assertThat(response.getData().assignedTo()).isEqualTo("alice"))
                .verifyComplete();

        verify(routingService).routeWorkItem(7L, 400L, command, "operator-1");
    }

    @Test
    void slaEndpointsPassTenantOperatorAndBoundedLimit() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationRoutingService routingService = mock(ConversationRoutingService.class);
        when(routingService.evaluateSlaBreaches(7L, null, "operator-1", 100))
                .thenReturn(slaEvaluationView());
        when(routingService.slaBreaches(7L, "OPEN", 100)).thenReturn(List.of(slaBreachView()));
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService, resolver());

        StepVerifier.create(controller.evaluateSlaBreaches(500))
                .assertNext(response -> assertThat(response.getData().created()).isEqualTo(1))
                .verifyComplete();
        StepVerifier.create(controller.slaBreaches("OPEN", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(breach -> assertThat(breach.workItemId()).isEqualTo(400L)))
                .verifyComplete();

        verify(routingService).evaluateSlaBreaches(7L, null, "operator-1", 100);
        verify(routingService).slaBreaches(7L, "OPEN", 100);
    }

    @Test
    void aiReplyGenerateEndpointPassesTenantOperatorAndCommand() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationAiReplyService aiReplyService = mock(ConversationAiReplyService.class);
        ConversationAiReplyGenerateCommand command = new ConversationAiReplyGenerateCommand(
                3L, 8L, "support-reply-v1", "empathetic", "REFUND_POLICY",
                Map.of("temperature", 0.2), 2_000, "short reply");
        when(aiReplyService.generate(7L, 400L, command, "operator-1")).thenReturn(aiSuggestion());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), aiReplyService, resolver());

        StepVerifier.create(controller.generateAiReplySuggestion(400L, command))
                .assertNext(response -> assertThat(response.getData().suggestedReplyText()).contains("退款政策"))
                .verifyComplete();

        verify(aiReplyService).generate(7L, 400L, command, "operator-1");
    }

    @Test
    void aiReplyReviewEndpointPassesTenantOperatorAndCommand() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationAiReplyService aiReplyService = mock(ConversationAiReplyService.class);
        ConversationAiReplyReviewCommand command = new ConversationAiReplyReviewCommand("ACCEPTED", "ok");
        when(aiReplyService.review(7L, 400L, 700L, command, "operator-1")).thenReturn(aiSuggestion());
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), aiReplyService, resolver());

        StepVerifier.create(controller.reviewAiReplySuggestion(400L, 700L, command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRAFT"))
                .verifyComplete();

        verify(aiReplyService).review(7L, 400L, 700L, command, "operator-1");
    }

    @Test
    void aiReplyListEndpointPassesTenantStatusAndBoundedLimit() {
        ConversationWorkspaceService service = mock(ConversationWorkspaceService.class);
        ConversationAiReplyService aiReplyService = mock(ConversationAiReplyService.class);
        when(aiReplyService.list(7L, 400L, new ConversationAiReplySuggestionQuery("DRAFT", 100)))
                .thenReturn(List.of(aiSuggestion()));
        ConversationWorkspaceController controller = new ConversationWorkspaceController(
                service, routingService(), aiReplyService, resolver());

        StepVerifier.create(controller.aiReplySuggestions(400L, "DRAFT", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(suggestion -> assertThat(suggestion.id()).isEqualTo(700L)))
                .verifyComplete();

        verify(aiReplyService).list(7L, 400L, new ConversationAiReplySuggestionQuery("DRAFT", 100));
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private ConversationRoutingService routingService() {
        return mock(ConversationRoutingService.class);
    }

    private ConversationRoutingAgentCommand agentCommand() {
        return new ConversationRoutingAgentCommand(
                "alice",
                "Alice",
                "sales",
                "AVAILABLE",
                3,
                1,
                List.of("sales", "vip"),
                Map.of("region", "east"));
    }

    private ConversationRoutingRuleCommand ruleCommand() {
        return new ConversationRoutingRuleCommand(
                "vip-sales",
                "WEB_CHAT",
                "HIGH",
                List.of("sales", "vip"),
                "sales",
                30,
                true,
                10,
                Map.of("segment", "vip"));
    }

    private ConversationWorkItemView workItem() {
        return new ConversationWorkItemView(
                400L,
                7L,
                100L,
                300L,
                "user-1",
                "WEB_CHAT",
                "WIDGET",
                "WEB_CHAT conversation with user-1",
                "OPEN",
                "NORMAL",
                "alice",
                "sales",
                "CONVERSATION",
                null,
                null,
                now(),
                now(),
                List.of(),
                Map.of(),
                now(),
                now());
    }

    private ConversationSopTaskView task() {
        return new ConversationSopTaskView(
                500L,
                7L,
                400L,
                "book_demo",
                "Book a demo",
                "TODO",
                "alice",
                now().plusHours(2),
                null,
                null,
                Map.of("playbook", "sales_handoff"),
                now(),
                now());
    }

    private ConversationRoutingAgentView agentView() {
        return new ConversationRoutingAgentView(
                10L,
                7L,
                "alice",
                "Alice",
                "sales",
                "AVAILABLE",
                3,
                1,
                List.of("sales", "vip"),
                Map.of("region", "east"),
                "operator-1",
                now(),
                now());
    }

    private ConversationRoutingRuleView ruleView() {
        return new ConversationRoutingRuleView(
                20L,
                7L,
                "vip-sales",
                "WEB_CHAT",
                "HIGH",
                List.of("sales", "vip"),
                "sales",
                30,
                true,
                10,
                Map.of("segment", "vip"),
                "operator-1",
                now(),
                now());
    }

    private ConversationRouteResultView routeView() {
        return new ConversationRouteResultView(
                7L,
                400L,
                true,
                "alice",
                "sales",
                "ROUTED",
                "matched rule vip-sales",
                List.of("sales", "vip"),
                now().plusMinutes(30));
    }

    private ConversationSlaEvaluationView slaEvaluationView() {
        return new ConversationSlaEvaluationView(7L, 2, 1, 1, List.of(slaBreachView()));
    }

    private ConversationSlaBreachView slaBreachView() {
        return new ConversationSlaBreachView(
                900L,
                7L,
                400L,
                "FIRST_RESPONSE",
                "HIGH",
                "OPEN",
                "sales",
                "SLA due time breached",
                now().minusMinutes(5),
                now(),
                null,
                null,
                Map.of("priority", "HIGH"),
                now(),
                now());
    }

    private ConversationAiReplySuggestionView aiSuggestion() {
        return new ConversationAiReplySuggestionView(
                700L,
                7L,
                400L,
                100L,
                202L,
                Map.of("workItemId", 400),
                "理解您的情况。我先帮您核实退款政策。",
                "empathetic",
                "REFUND_POLICY",
                0.86,
                List.of("SENSITIVE_REFUND"),
                List.of("客户询问退款政策"),
                3L,
                8L,
                "support-reply-v1",
                "SUCCESS",
                false,
                "DRAFT",
                "operator-1",
                null,
                null,
                null,
                now(),
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 11, 0);
    }
}
