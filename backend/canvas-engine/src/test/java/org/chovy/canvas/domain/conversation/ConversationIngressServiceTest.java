package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationMessageDO;
import org.chovy.canvas.dal.dataobject.ConversationSessionDO;
import org.chovy.canvas.dal.mapper.ConversationMessageMapper;
import org.chovy.canvas.dal.mapper.ConversationSessionMapper;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationIngressServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 10, 0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void firstInboundReplyCreatesSessionMessageAndResumesConversationWaits() throws Exception {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        WaitResumeService waitResumeService = mock(WaitResumeService.class);
        when(messageMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(sessionMapper.insert(any(ConversationSessionDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationSessionDO>getArgument(0).setId(100L);
            return 1;
        });
        when(messageMapper.insert(any(ConversationMessageDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationMessageDO>getArgument(0).setId(200L);
            return 1;
        });
        when(waitResumeService.resumeEventWaits(eq("CONVERSATION_REPLY"), eq("user-1"), any(), eq("evt-1")))
                .thenReturn(2);
        ConversationIngressService service = service(sessionMapper, messageMapper, waitResumeService);

        ConversationIngressResp resp = service.ingest(7L, ingress(" whatsapp ", "twilio", "msg-1", "evt-1", "text", "PRODUCT_A"));

        assertThat(resp.sessionId()).isEqualTo(100L);
        assertThat(resp.messageId()).isEqualTo(200L);
        assertThat(resp.status()).isEqualTo("RECORDED");
        assertThat(resp.duplicate()).isFalse();
        assertThat(resp.resumedWaitCount()).isEqualTo(2);

        ArgumentCaptor<ConversationSessionDO> sessionCaptor = ArgumentCaptor.forClass(ConversationSessionDO.class);
        verify(sessionMapper).insert(sessionCaptor.capture());
        ConversationSessionDO session = sessionCaptor.getValue();
        assertThat(session.getTenantId()).isEqualTo(7L);
        assertThat(session.getCanvasId()).isEqualTo(10L);
        assertThat(session.getVersionId()).isEqualTo(20L);
        assertThat(session.getExecutionId()).isEqualTo("exec-1");
        assertThat(session.getUserId()).isEqualTo("user-1");
        assertThat(session.getChannel()).isEqualTo("WHATSAPP");
        assertThat(session.getProvider()).isEqualTo("TWILIO");
        assertThat(session.getStatus()).isEqualTo("ACTIVE");
        assertThat(session.getTurnCount()).isEqualTo(0);
        assertThat(session.getLastMessageAt()).isEqualTo(NOW);

        ArgumentCaptor<ConversationMessageDO> messageCaptor = ArgumentCaptor.forClass(ConversationMessageDO.class);
        verify(messageMapper).insert(messageCaptor.capture());
        ConversationMessageDO message = messageCaptor.getValue();
        assertThat(message.getTenantId()).isEqualTo(7L);
        assertThat(message.getSessionId()).isEqualTo(100L);
        assertThat(message.getDirection()).isEqualTo("INBOUND");
        assertThat(message.getMessageType()).isEqualTo("TEXT");
        assertThat(message.getExternalMessageId()).isEqualTo("msg-1");
        assertThat(message.getIdempotencyKey()).isEqualTo("WHATSAPP:TWILIO:msg-1");
        assertThat(message.getTextContent()).isEqualTo("yes please");
        assertThat(message.getIntent()).isEqualTo("PRODUCT_A");
        Map<String, Object> content = OBJECT_MAPPER.readValue(message.getContentJson(), new TypeReference<>() {});
        assertThat(content).containsEntry("text", "yes please")
                .containsEntry("intent", "PRODUCT_A");
        assertThat((Map<String, Object>) content.get("attributes")).containsEntry("locale", "en-US");

        ArgumentCaptor<ConversationSessionDO> updateCaptor = ArgumentCaptor.forClass(ConversationSessionDO.class);
        verify(sessionMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(updateCaptor.getValue().getTurnCount()).isEqualTo(1);
        Map<String, Object> context = OBJECT_MAPPER.readValue(updateCaptor.getValue().getContextJson(), new TypeReference<>() {});
        assertThat(context).containsEntry("intent", "PRODUCT_A")
                .containsEntry("lastText", "yes please")
                .containsEntry("lastMessageId", 200);

        ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(waitResumeService).resumeEventWaits(eq("CONVERSATION_REPLY"), eq("user-1"),
                attributesCaptor.capture(), eq("evt-1"));
        assertThat(attributesCaptor.getValue()).containsEntry("channel", "WHATSAPP")
                .containsEntry("provider", "TWILIO")
                .containsEntry("intent", "PRODUCT_A")
                .containsEntry("text", "yes please")
                .containsEntry("sessionId", 100L)
                .containsEntry("messageId", 200L)
                .containsEntry("conversationSessionId", 100L)
                .containsEntry("conversationMessageId", 200L)
                .containsEntry("locale", "en-US");
        assertThat((Map<String, Object>) attributesCaptor.getValue().get("attributes"))
                .containsEntry("locale", "en-US");
    }

    @Test
    void ingressServiceDoesNotDependOnWorkspaceSlice() {
        assertThat(Arrays.stream(ConversationIngressService.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .map(Class::getName))
                .doesNotContain(
                        "org.chovy.canvas.domain.conversation.ConversationWorkspaceService",
                        "org.springframework.beans.factory.ObjectProvider");
    }

    @Test
    void duplicateInboundReplyReturnsExistingMessageWithoutResumingWaits() {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        WaitResumeService waitResumeService = mock(WaitResumeService.class);
        ConversationMessageDO existingMessage = message(200L, 100L, 7L);
        ConversationSessionDO existingSession = session(100L, 7L, "WHATSAPP", "TWILIO", 3);
        when(messageMapper.selectOne(any(Wrapper.class))).thenReturn(existingMessage);
        when(sessionMapper.selectById(100L)).thenReturn(existingSession);
        ConversationIngressService service = service(sessionMapper, messageMapper, waitResumeService);

        ConversationIngressResp resp = service.ingest(7L, ingress("WHATSAPP", "TWILIO", "msg-1", "evt-1", "TEXT", "PRODUCT_A"));

        assertThat(resp.sessionId()).isEqualTo(100L);
        assertThat(resp.messageId()).isEqualTo(200L);
        assertThat(resp.status()).isEqualTo("RECORDED");
        assertThat(resp.duplicate()).isTrue();
        assertThat(resp.resumedWaitCount()).isZero();
        verify(sessionMapper, never()).insert(any(ConversationSessionDO.class));
        verify(messageMapper, never()).insert(any(ConversationMessageDO.class));
        verify(waitResumeService, never()).resumeEventWaits(any(), any(), any(), any());
    }

    @Test
    void activeSessionUpdateIncrementsTurnCountAndMergesLatestIntent() throws Exception {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        WaitResumeService waitResumeService = mock(WaitResumeService.class);
        ConversationSessionDO existingSession = session(100L, 7L, "WHATSAPP", "DEFAULT", 4);
        existingSession.setContextJson("{\"segment\":\"vip\",\"intent\":\"OLD\"}");
        when(messageMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(sessionMapper.selectOne(any(Wrapper.class))).thenReturn(existingSession);
        when(messageMapper.insert(any(ConversationMessageDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationMessageDO>getArgument(0).setId(201L);
            return 1;
        });
        when(waitResumeService.resumeEventWaits(any(), any(), any(), any())).thenReturn(0);
        ConversationIngressService service = service(sessionMapper, messageMapper, waitResumeService);

        ConversationIngressResp resp = service.ingest(7L,
                ingress("whatsapp", null, "msg-2", "evt-2", "text", "PRODUCT_B"));

        assertThat(resp.sessionId()).isEqualTo(100L);
        verify(sessionMapper, never()).insert(any(ConversationSessionDO.class));
        ArgumentCaptor<ConversationSessionDO> updateCaptor = ArgumentCaptor.forClass(ConversationSessionDO.class);
        verify(sessionMapper).updateById(updateCaptor.capture());
        ConversationSessionDO update = updateCaptor.getValue();
        assertThat(update.getId()).isEqualTo(100L);
        assertThat(update.getTurnCount()).isEqualTo(5);
        assertThat(update.getLastMessageAt()).isEqualTo(NOW);
        Map<String, Object> context = OBJECT_MAPPER.readValue(update.getContextJson(), new TypeReference<>() {});
        assertThat(context).containsEntry("segment", "vip")
                .containsEntry("intent", "PRODUCT_B")
                .containsEntry("lastText", "yes please")
                .containsEntry("lastMessageId", 201);
    }

    @Test
    void recentSessionsReturnOnlyRequestedTenant() {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        when(sessionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                session(100L, 7L, "WHATSAPP", "DEFAULT", 2),
                session(101L, 8L, "WHATSAPP", "DEFAULT", 1)));
        ConversationIngressService service = service(sessionMapper, messageMapper, mock(WaitResumeService.class));

        List<ConversationSessionView> sessions = service.listRecentSessions(7L, "user-1", "whatsapp", 25);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).id()).isEqualTo(100L);
        assertThat(sessions.get(0).tenantId()).isEqualTo(7L);
        assertThat(sessions.get(0).channel()).isEqualTo("WHATSAPP");
        verify(sessionMapper).selectList(any(Wrapper.class));
    }

    @Test
    void messageListingRejectsSessionFromAnotherTenant() {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        when(sessionMapper.selectById(100L)).thenReturn(session(100L, 8L, "WHATSAPP", "DEFAULT", 1));
        ConversationIngressService service = service(sessionMapper, messageMapper, mock(WaitResumeService.class));

        assertThatThrownBy(() -> service.listMessages(7L, 100L, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation session not found");
        verify(messageMapper, never()).selectList(any());
    }

    private ConversationIngressService service(ConversationSessionMapper sessionMapper,
                                               ConversationMessageMapper messageMapper,
                                               WaitResumeService waitResumeService) {
        return new ConversationIngressService(sessionMapper, messageMapper, waitResumeService, OBJECT_MAPPER, CLOCK);
    }

    private static ConversationIngressReq ingress(String channel,
                                                  String provider,
                                                  String externalMessageId,
                                                  String eventId,
                                                  String messageType,
                                                  String intent) {
        return new ConversationIngressReq(
                10L,
                20L,
                "exec-1",
                "user-1",
                channel,
                provider,
                externalMessageId,
                eventId,
                messageType,
                "yes please",
                intent,
                Map.of("locale", "en-US"),
                NOW);
    }

    private static ConversationSessionDO session(Long id, Long tenantId, String channel, String provider, int turns) {
        ConversationSessionDO session = new ConversationSessionDO();
        session.setId(id);
        session.setTenantId(tenantId);
        session.setCanvasId(10L);
        session.setVersionId(20L);
        session.setExecutionId("exec-1");
        session.setUserId("user-1");
        session.setChannel(channel);
        session.setProvider(provider);
        session.setStatus("ACTIVE");
        session.setTurnCount(turns);
        session.setContextJson("{}");
        session.setLastMessageAt(NOW.minusMinutes(5));
        session.setCreatedAt(NOW.minusMinutes(5));
        session.setUpdatedAt(NOW.minusMinutes(5));
        return session;
    }

    private static ConversationMessageDO message(Long id, Long sessionId, Long tenantId) {
        ConversationMessageDO message = new ConversationMessageDO();
        message.setId(id);
        message.setTenantId(tenantId);
        message.setSessionId(sessionId);
        message.setDirection("INBOUND");
        message.setMessageType("TEXT");
        message.setExternalMessageId("msg-1");
        message.setIdempotencyKey("WHATSAPP:TWILIO:msg-1");
        message.setContentJson("{\"text\":\"yes please\"}");
        message.setTextContent("yes please");
        message.setIntent("PRODUCT_A");
        message.setCreatedAt(NOW);
        return message;
    }
}
