package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationAiReplySuggestionDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO;
import org.chovy.canvas.dal.mapper.ConversationAiReplySuggestionMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationAiReplyServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void generateStoresDraftFromLatestInboundMessageAndAudits() throws Exception {
        Harness harness = harness(new FixedGenerator(new ConversationAiReplyGenerationResult(
                "理解您的情况。我先帮您核实退款政策，稍后给您明确答复。",
                "empathetic",
                "REFUND_POLICY",
                0.86,
                List.of("REFUND_POLICY_REVIEW"),
                List.of("客户询问退款政策"),
                3L,
                8L,
                "support-reply-v1",
                "SUCCESS",
                false)));
        when(harness.workspaceService.timeline(7L, 400L, 50, 20)).thenReturn(timelineWithRefund());
        when(harness.suggestionMapper.insert(any(ConversationAiReplySuggestionDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationAiReplySuggestionDO>getArgument(0).setId(700L);
            return 1;
        });

        ConversationAiReplySuggestionView view = harness.service.generate(7L, 400L,
                new ConversationAiReplyGenerateCommand(3L, 8L, "support-reply-v1", "empathetic",
                        "REFUND_POLICY", Map.of("temperature", 0.2), 2_000, "请简短回复"),
                "operator-1");

        assertThat(view.id()).isEqualTo(700L);
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.sourceMessageId()).isEqualTo(202L);
        assertThat(view.suggestedReplyText()).contains("退款政策");
        assertThat(view.riskFlags()).contains("REFUND_POLICY_REVIEW", "SENSITIVE_REFUND");
        assertThat(view.groundingSnippets()).contains("客户询问退款政策");
        assertThat(harness.generator.context().messages()).hasSize(3);
        assertThat(harness.generator.context().command().operatorInstruction()).isEqualTo("请简短回复");

        ArgumentCaptor<ConversationAiReplySuggestionDO> suggestionCaptor =
                ArgumentCaptor.forClass(ConversationAiReplySuggestionDO.class);
        verify(harness.suggestionMapper).insert(suggestionCaptor.capture());
        ConversationAiReplySuggestionDO row = suggestionCaptor.getValue();
        assertThat(row.getTenantId()).isEqualTo(7L);
        assertThat(row.getWorkItemId()).isEqualTo(400L);
        assertThat(row.getSessionId()).isEqualTo(100L);
        assertThat(row.getSourceMessageId()).isEqualTo(202L);
        assertThat(row.getStatus()).isEqualTo("DRAFT");
        assertThat(row.getGeneratedBy()).isEqualTo("operator-1");
        assertThat(row.getCreatedAt()).isEqualTo(NOW);
        assertThat(readMap(row.getPromptContextJson())).containsEntry("workItemId", 400);
        assertThat(readList(row.getRiskFlagsJson())).contains("SENSITIVE_REFUND");

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("AI_REPLY_SUGGESTED");
        assertThat(auditCaptor.getValue().getActor()).isEqualTo("operator-1");
        assertThat(readMap(auditCaptor.getValue().getNewValueJson()))
                .containsEntry("suggestionId", 700)
                .containsEntry("status", "DRAFT");
    }

    @Test
    void generateFlagsMissingContextWithoutSourceMessage() throws Exception {
        Harness harness = harness(new FixedGenerator(new ConversationAiReplyGenerationResult(
                "我需要再确认一下上下文后回复您。",
                "neutral",
                "NEEDS_CONTEXT",
                0.42,
                List.of(),
                List.of(),
                null,
                null,
                "fallback",
                "FALLBACK",
                true)));
        when(harness.workspaceService.timeline(7L, 400L, 50, 20)).thenReturn(timelineWithoutMessages());
        when(harness.suggestionMapper.insert(any(ConversationAiReplySuggestionDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationAiReplySuggestionDO>getArgument(0).setId(701L);
            return 1;
        });

        ConversationAiReplySuggestionView view = harness.service.generate(7L, 400L,
                new ConversationAiReplyGenerateCommand(null, null, null, null, null, Map.of(), null, null),
                "operator-1");

        assertThat(view.sourceMessageId()).isNull();
        assertThat(view.riskFlags()).contains("MISSING_CONTEXT", "LOW_CONFIDENCE");
        ArgumentCaptor<ConversationAiReplySuggestionDO> suggestionCaptor =
                ArgumentCaptor.forClass(ConversationAiReplySuggestionDO.class);
        verify(harness.suggestionMapper).insert(suggestionCaptor.capture());
        assertThat(readList(suggestionCaptor.getValue().getRiskFlagsJson()))
                .contains("MISSING_CONTEXT", "LOW_CONFIDENCE");
    }

    @Test
    void reviewAcceptsDraftSuggestionAndAuditsDecision() throws Exception {
        Harness harness = harness(new FixedGenerator(null));
        when(harness.suggestionMapper.selectById(700L)).thenReturn(suggestion(700L, 7L, 400L, "DRAFT"));

        ConversationAiReplySuggestionView view = harness.service.review(7L, 400L, 700L,
                new ConversationAiReplyReviewCommand("ACCEPTED", "可以发送"),
                "operator-2");

        assertThat(view.status()).isEqualTo("ACCEPTED");
        assertThat(view.reviewedBy()).isEqualTo("operator-2");
        assertThat(view.reviewedAt()).isEqualTo(NOW);

        ArgumentCaptor<ConversationAiReplySuggestionDO> updateCaptor =
                ArgumentCaptor.forClass(ConversationAiReplySuggestionDO.class);
        verify(harness.suggestionMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("ACCEPTED");
        assertThat(updateCaptor.getValue().getReviewedBy()).isEqualTo("operator-2");
        assertThat(updateCaptor.getValue().getReviewNote()).isEqualTo("可以发送");

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("AI_REPLY_REVIEWED");
        assertThat(readMap(auditCaptor.getValue().getNewValueJson()))
                .containsEntry("suggestionId", 700)
                .containsEntry("status", "ACCEPTED");
    }

    @Test
    void rejectsCrossTenantSuggestionReview() {
        Harness harness = harness(new FixedGenerator(null));
        when(harness.suggestionMapper.selectById(700L)).thenReturn(suggestion(700L, 99L, 400L, "DRAFT"));

        assertThatThrownBy(() -> harness.service.review(7L, 400L, 700L,
                new ConversationAiReplyReviewCommand("REJECTED", "wrong tenant"),
                "operator-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AI reply suggestion not found");

        verify(harness.suggestionMapper, never()).updateById(any(ConversationAiReplySuggestionDO.class));
        verify(harness.auditMapper, never()).insert(any(ConversationWorkItemAuditDO.class));
    }

    @Test
    void listReturnsTenantScopedSuggestionViews() {
        Harness harness = harness(new FixedGenerator(null));
        when(harness.suggestionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                suggestion(700L, 7L, 400L, "DRAFT"),
                suggestion(701L, 7L, 400L, "REJECTED")));

        List<ConversationAiReplySuggestionView> views = harness.service.list(7L, 400L,
                new ConversationAiReplySuggestionQuery(null, 500));

        assertThat(views).extracting(ConversationAiReplySuggestionView::id)
                .containsExactly(700L, 701L);
        assertThat(views).allSatisfy(view -> assertThat(view.tenantId()).isEqualTo(7L));
        verify(harness.suggestionMapper).selectList(any(Wrapper.class));
    }

    private Harness harness(FixedGenerator generator) {
        ConversationWorkspaceService workspaceService = mock(ConversationWorkspaceService.class);
        ConversationAiReplySuggestionMapper suggestionMapper = mock(ConversationAiReplySuggestionMapper.class);
        ConversationWorkItemAuditMapper auditMapper = mock(ConversationWorkItemAuditMapper.class);
        ConversationAiReplyService service = new ConversationAiReplyService(
                workspaceService,
                suggestionMapper,
                auditMapper,
                generator,
                OBJECT_MAPPER,
                CLOCK);
        return new Harness(service, workspaceService, suggestionMapper, auditMapper, generator);
    }

    private static ConversationWorkspaceTimelineView timelineWithRefund() {
        return new ConversationWorkspaceTimelineView(
                workItem(),
                contact(),
                session(),
                List.of(
                        message(200L, "INBOUND", "您好，我想了解会员权益", "GENERAL"),
                        message(201L, "OUTBOUND", "您好，可以帮您介绍。", null),
                        message(202L, "INBOUND", "如果不合适可以退款吗？", "REFUND_POLICY")),
                List.of(task()),
                List.of());
    }

    private static ConversationWorkspaceTimelineView timelineWithoutMessages() {
        return new ConversationWorkspaceTimelineView(
                workItem(),
                contact(),
                session(),
                List.of(),
                List.of(),
                List.of());
    }

    private static ConversationWorkItemView workItem() {
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
                "HIGH",
                "alice",
                "sales",
                "CONVERSATION",
                NOW.plusMinutes(30),
                null,
                NOW.minusMinutes(2),
                null,
                List.of("vip"),
                Map.of("segment", "vip"),
                "ROUTED",
                List.of("sales"),
                "matched vip rule",
                NOW.minusMinutes(1),
                "vip-sales",
                NOW.minusMinutes(10),
                NOW.minusMinutes(1));
    }

    private static ConversationContactProfileView contact() {
        return new ConversationContactProfileView(
                300L,
                7L,
                "user-1",
                "王女士",
                "external-1",
                "WEB_CHAT",
                "alice",
                "VIP",
                List.of("vip"),
                Map.of("city", "Shanghai"),
                NOW.minusDays(3),
                NOW.minusMinutes(1));
    }

    private static ConversationSessionView session() {
        return new ConversationSessionView(
                100L,
                7L,
                10L,
                20L,
                "exec-1",
                "user-1",
                "WEB_CHAT",
                "WIDGET",
                "ACTIVE",
                3,
                Map.of("entry", "pricing"),
                NOW.minusMinutes(1),
                NOW.plusHours(1),
                NOW.minusMinutes(15),
                NOW.minusMinutes(1));
    }

    private static ConversationMessageView message(Long id, String direction, String text, String intent) {
        return new ConversationMessageView(
                id,
                7L,
                100L,
                direction,
                "TEXT",
                "msg-" + id,
                text,
                intent,
                Map.of("text", text),
                NOW.minusMinutes(202L - id));
    }

    private static ConversationSopTaskView task() {
        return new ConversationSopTaskView(
                500L,
                7L,
                400L,
                "confirm_policy",
                "Confirm refund policy",
                "TODO",
                "alice",
                NOW.plusMinutes(10),
                null,
                null,
                Map.of("playbook", "refund"),
                NOW.minusMinutes(1),
                NOW.minusMinutes(1));
    }

    private static ConversationAiReplySuggestionDO suggestion(Long id, Long tenantId, Long workItemId, String status) {
        ConversationAiReplySuggestionDO suggestion = new ConversationAiReplySuggestionDO();
        suggestion.setId(id);
        suggestion.setTenantId(tenantId);
        suggestion.setWorkItemId(workItemId);
        suggestion.setSessionId(100L);
        suggestion.setSourceMessageId(202L);
        suggestion.setPromptContextJson("{\"workItemId\":400}");
        suggestion.setSuggestedReplyText("理解您的情况。我先帮您核实退款政策。");
        suggestion.setTone("empathetic");
        suggestion.setIntent("REFUND_POLICY");
        suggestion.setConfidence(0.86);
        suggestion.setRiskFlagsJson("[\"SENSITIVE_REFUND\"]");
        suggestion.setGroundingSnippetsJson("[\"客户询问退款政策\"]");
        suggestion.setProviderId(3L);
        suggestion.setTemplateId(8L);
        suggestion.setModelKey("support-reply-v1");
        suggestion.setProviderStatus("SUCCESS");
        suggestion.setFallbackUsed(false);
        suggestion.setStatus(status);
        suggestion.setGeneratedBy("operator-1");
        suggestion.setCreatedAt(NOW.minusMinutes(1));
        suggestion.setUpdatedAt(NOW.minusMinutes(1));
        return suggestion;
    }

    private static Map<String, Object> readMap(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private static List<String> readList(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private static final class FixedGenerator implements ConversationAiReplyGenerator {
        private final ConversationAiReplyGenerationResult result;
        private ConversationAiReplyGenerationContext context;

        private FixedGenerator(ConversationAiReplyGenerationResult result) {
            this.result = result;
        }

        @Override
        public ConversationAiReplyGenerationResult generate(ConversationAiReplyGenerationContext context) {
            this.context = context;
            return result;
        }

        ConversationAiReplyGenerationContext context() {
            return context;
        }
    }

    private record Harness(
            ConversationAiReplyService service,
            ConversationWorkspaceService workspaceService,
            ConversationAiReplySuggestionMapper suggestionMapper,
            ConversationWorkItemAuditMapper auditMapper,
            FixedGenerator generator) {
    }
}
