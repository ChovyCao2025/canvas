package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationContactProfileDO;
import org.chovy.canvas.dal.dataobject.ConversationMessageDO;
import org.chovy.canvas.dal.dataobject.ConversationSessionDO;
import org.chovy.canvas.dal.dataobject.ConversationSopTaskDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemAuditDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemDO;
import org.chovy.canvas.dal.mapper.ConversationContactProfileMapper;
import org.chovy.canvas.dal.mapper.ConversationMessageMapper;
import org.chovy.canvas.dal.mapper.ConversationSessionMapper;
import org.chovy.canvas.dal.mapper.ConversationSopTaskMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemAuditMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
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

class ConversationWorkspaceServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T03:00:00Z"),
            ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void ensureWorkItemForSessionCreatesContactWorkItemAndAuditIdempotently() throws Exception {
        Harness harness = harness();
        when(harness.sessionMapper.selectById(100L)).thenReturn(session(100L, 7L));
        when(harness.workItemMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.contactProfileMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(harness.contactProfileMapper.insert(any(ConversationContactProfileDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationContactProfileDO>getArgument(0).setId(300L);
            return 1;
        });
        when(harness.workItemMapper.insert(any(ConversationWorkItemDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationWorkItemDO>getArgument(0).setId(400L);
            return 1;
        });

        ConversationWorkItemView view = harness.service.ensureWorkItemForSession(7L, 100L, "operator-1");

        assertThat(view.id()).isEqualTo(400L);
        assertThat(view.contactProfileId()).isEqualTo(300L);
        assertThat(view.status()).isEqualTo("OPEN");
        assertThat(view.priority()).isEqualTo("NORMAL");
        assertThat(view.subject()).isEqualTo("WEB_CHAT conversation with user-1");

        ArgumentCaptor<ConversationContactProfileDO> contactCaptor =
                ArgumentCaptor.forClass(ConversationContactProfileDO.class);
        verify(harness.contactProfileMapper).insert(contactCaptor.capture());
        assertThat(contactCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(contactCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(contactCaptor.getValue().getPrivateDomainSource()).isEqualTo("WEB_CHAT");

        ArgumentCaptor<ConversationWorkItemDO> workItemCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        verify(harness.workItemMapper).insert(workItemCaptor.capture());
        assertThat(workItemCaptor.getValue().getSessionId()).isEqualTo(100L);
        assertThat(workItemCaptor.getValue().getChannel()).isEqualTo("WEB_CHAT");
        assertThat(workItemCaptor.getValue().getLastCustomerMessageAt()).isEqualTo(NOW.minusMinutes(3));

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("CREATED");
        assertThat(auditCaptor.getValue().getActor()).isEqualTo("operator-1");
        Map<String, Object> newValues = readMap(auditCaptor.getValue().getNewValueJson());
        assertThat(newValues).containsEntry("status", "OPEN")
                .containsEntry("priority", "NORMAL");
    }

    @Test
    void ensureWorkItemReturnsExistingWorkItemWithoutDuplicateInsert() {
        Harness harness = harness();
        when(harness.sessionMapper.selectById(100L)).thenReturn(session(100L, 7L));
        when(harness.workItemMapper.selectOne(any(Wrapper.class))).thenReturn(workItem(400L, 7L, 100L));

        ConversationWorkItemView view = harness.service.ensureWorkItemForSession(7L, 100L, "operator-1");

        assertThat(view.id()).isEqualTo(400L);
        verify(harness.contactProfileMapper, never()).insert(any(ConversationContactProfileDO.class));
        verify(harness.workItemMapper, never()).insert(any(ConversationWorkItemDO.class));
        verify(harness.auditMapper, never()).insert(any(ConversationWorkItemAuditDO.class));
    }

    @Test
    void assignWorkItemUpdatesOwnerTeamActivityAndAudit() throws Exception {
        Harness harness = harness();
        ConversationWorkItemDO row = workItem(400L, 7L, 100L);
        row.setAssignedTo("old-owner");
        row.setAssignedTeam("old-team");
        when(harness.workItemMapper.selectById(400L)).thenReturn(row);

        ConversationWorkItemView view = harness.service.assign(7L, 400L,
                new ConversationAssignmentCommand("alice", "sales", "VIP pricing request"),
                "manager-1");

        assertThat(view.assignedTo()).isEqualTo("alice");
        assertThat(view.assignedTeam()).isEqualTo("sales");

        ArgumentCaptor<ConversationWorkItemDO> updateCaptor = ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        verify(harness.workItemMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(400L);
        assertThat(updateCaptor.getValue().getAssignedTo()).isEqualTo("alice");
        assertThat(updateCaptor.getValue().getAssignedTeam()).isEqualTo("sales");
        assertThat(updateCaptor.getValue().getLastOperatorActivityAt()).isEqualTo(NOW);

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("ASSIGNED");
        assertThat(auditCaptor.getValue().getActor()).isEqualTo("manager-1");
        assertThat(auditCaptor.getValue().getNote()).isEqualTo("VIP pricing request");
        assertThat(readMap(auditCaptor.getValue().getOldValueJson())).containsEntry("assignedTo", "old-owner");
        assertThat(readMap(auditCaptor.getValue().getNewValueJson())).containsEntry("assignedTo", "alice");
    }

    @Test
    void updateStatusStoresPriorityReminderAndAudit() {
        Harness harness = harness();
        ConversationWorkItemDO row = workItem(400L, 7L, 100L);
        when(harness.workItemMapper.selectById(400L)).thenReturn(row);
        LocalDateTime followUpAt = NOW.plusDays(1);

        ConversationWorkItemView view = harness.service.updateStatus(7L, 400L,
                new ConversationWorkItemStatusCommand("SNOOZED", "HIGH", followUpAt, "follow after demo"),
                "operator-1");

        assertThat(view.status()).isEqualTo("SNOOZED");
        assertThat(view.priority()).isEqualTo("HIGH");
        assertThat(view.nextFollowUpAt()).isEqualTo(followUpAt);

        ArgumentCaptor<ConversationWorkItemDO> updateCaptor = ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        verify(harness.workItemMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("SNOOZED");
        assertThat(updateCaptor.getValue().getPriority()).isEqualTo("HIGH");
        assertThat(updateCaptor.getValue().getNextFollowUpAt()).isEqualTo(followUpAt);

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("STATUS_CHANGED");
        assertThat(auditCaptor.getValue().getNote()).isEqualTo("follow after demo");
    }

    @Test
    void createAndCompleteSopTaskPersistLifecycleAndAudit() {
        Harness harness = harness();
        when(harness.workItemMapper.selectById(400L)).thenReturn(workItem(400L, 7L, 100L));
        when(harness.taskMapper.insert(any(ConversationSopTaskDO.class))).thenAnswer(invocation -> {
            invocation.<ConversationSopTaskDO>getArgument(0).setId(500L);
            return 1;
        });

        ConversationSopTaskView created = harness.service.createTask(7L, 400L,
                new ConversationSopTaskCommand(
                        "book_demo",
                        "Book a product demo",
                        "alice",
                        NOW.plusHours(2),
                        Map.of("playbook", "sales_handoff")),
                "operator-1");

        assertThat(created.id()).isEqualTo(500L);
        assertThat(created.status()).isEqualTo("TODO");
        ArgumentCaptor<ConversationSopTaskDO> taskCaptor = ArgumentCaptor.forClass(ConversationSopTaskDO.class);
        verify(harness.taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("book_demo");
        assertThat(taskCaptor.getValue().getMetadataJson()).contains("sales_handoff");

        ConversationSopTaskDO task = task(500L, 7L, 400L);
        when(harness.taskMapper.selectById(500L)).thenReturn(task);
        ConversationSopTaskView completed = harness.service.completeTask(7L, 500L,
                new ConversationSopTaskCompletionCommand("Demo booked"),
                "alice");

        assertThat(completed.status()).isEqualTo("DONE");
        ArgumentCaptor<ConversationSopTaskDO> completeCaptor = ArgumentCaptor.forClass(ConversationSopTaskDO.class);
        verify(harness.taskMapper).updateById(completeCaptor.capture());
        assertThat(completeCaptor.getValue().getStatus()).isEqualTo("DONE");
        assertThat(completeCaptor.getValue().getCompletedBy()).isEqualTo("alice");
        assertThat(completeCaptor.getValue().getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void timelineRejectsWorkItemFromAnotherTenant() {
        Harness harness = harness();
        when(harness.workItemMapper.selectById(400L)).thenReturn(workItem(400L, 8L, 100L));

        assertThatThrownBy(() -> harness.service.timeline(7L, 400L, 50, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation work item not found");
    }

    @Test
    void inboundMessageUpdatesExistingWorkItemWithoutOverwritingAssignment() {
        Harness harness = harness();
        ConversationSessionDO session = session(100L, 7L);
        ConversationMessageDO message = message(200L, 7L, 100L);
        ConversationWorkItemDO existing = workItem(400L, 7L, 100L);
        existing.setAssignedTo("alice");
        existing.setAssignedTeam("sales");
        existing.setStatus("PENDING");
        when(harness.workItemMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        harness.service.recordInboundMessage(7L, session, message, ingress(), NOW);

        ArgumentCaptor<ConversationWorkItemDO> updateCaptor = ArgumentCaptor.forClass(ConversationWorkItemDO.class);
        verify(harness.workItemMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(400L);
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(updateCaptor.getValue().getLastCustomerMessageAt()).isEqualTo(NOW);
        assertThat(updateCaptor.getValue().getAssignedTo()).isNull();
        assertThat(updateCaptor.getValue().getAssignedTeam()).isNull();

        ArgumentCaptor<ConversationWorkItemAuditDO> auditCaptor =
                ArgumentCaptor.forClass(ConversationWorkItemAuditDO.class);
        verify(harness.auditMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("INBOUND_MESSAGE");
        assertThat(auditCaptor.getValue().getActor()).isEqualTo("system");
    }

    private Harness harness() {
        ConversationSessionMapper sessionMapper = mock(ConversationSessionMapper.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        ConversationContactProfileMapper contactProfileMapper = mock(ConversationContactProfileMapper.class);
        ConversationWorkItemMapper workItemMapper = mock(ConversationWorkItemMapper.class);
        ConversationSopTaskMapper taskMapper = mock(ConversationSopTaskMapper.class);
        ConversationWorkItemAuditMapper auditMapper = mock(ConversationWorkItemAuditMapper.class);
        ConversationWorkspaceService service = new ConversationWorkspaceService(
                sessionMapper,
                messageMapper,
                contactProfileMapper,
                workItemMapper,
                taskMapper,
                auditMapper,
                OBJECT_MAPPER,
                CLOCK);
        return new Harness(
                service,
                sessionMapper,
                messageMapper,
                contactProfileMapper,
                workItemMapper,
                taskMapper,
                auditMapper);
    }

    private static ConversationSessionDO session(Long id, Long tenantId) {
        ConversationSessionDO session = new ConversationSessionDO();
        session.setId(id);
        session.setTenantId(tenantId);
        session.setUserId("user-1");
        session.setChannel("WEB_CHAT");
        session.setProvider("WIDGET");
        session.setStatus("ACTIVE");
        session.setLastMessageAt(NOW.minusMinutes(3));
        session.setCreatedAt(NOW.minusMinutes(10));
        session.setUpdatedAt(NOW.minusMinutes(3));
        return session;
    }

    private static ConversationWorkItemDO workItem(Long id, Long tenantId, Long sessionId) {
        ConversationWorkItemDO workItem = new ConversationWorkItemDO();
        workItem.setId(id);
        workItem.setTenantId(tenantId);
        workItem.setSessionId(sessionId);
        workItem.setContactProfileId(300L);
        workItem.setUserId("user-1");
        workItem.setChannel("WEB_CHAT");
        workItem.setProvider("WIDGET");
        workItem.setSubject("WEB_CHAT conversation with user-1");
        workItem.setStatus("OPEN");
        workItem.setPriority("NORMAL");
        workItem.setSource("CONVERSATION");
        workItem.setTagsJson("[]");
        workItem.setAttributesJson("{}");
        workItem.setLastCustomerMessageAt(NOW.minusMinutes(3));
        workItem.setCreatedAt(NOW.minusMinutes(5));
        workItem.setUpdatedAt(NOW.minusMinutes(5));
        return workItem;
    }

    private static ConversationSopTaskDO task(Long id, Long tenantId, Long workItemId) {
        ConversationSopTaskDO task = new ConversationSopTaskDO();
        task.setId(id);
        task.setTenantId(tenantId);
        task.setWorkItemId(workItemId);
        task.setTaskKey("book_demo");
        task.setTitle("Book a product demo");
        task.setStatus("TODO");
        task.setAssignee("alice");
        task.setDueAt(NOW.plusHours(2));
        task.setMetadataJson("{}");
        task.setCreatedAt(NOW.minusMinutes(1));
        task.setUpdatedAt(NOW.minusMinutes(1));
        return task;
    }

    private static ConversationMessageDO message(Long id, Long tenantId, Long sessionId) {
        ConversationMessageDO message = new ConversationMessageDO();
        message.setId(id);
        message.setTenantId(tenantId);
        message.setSessionId(sessionId);
        message.setDirection("INBOUND");
        message.setMessageType("TEXT");
        message.setTextContent("I want a demo");
        message.setIntent("BOOK_DEMO");
        message.setCreatedAt(NOW);
        return message;
    }

    private static ConversationIngressReq ingress() {
        return new ConversationIngressReq(
                10L,
                20L,
                "exec-1",
                "user-1",
                "WEB_CHAT",
                "WIDGET",
                "web-msg-1",
                "web-event-1",
                "TEXT",
                "I want a demo",
                "BOOK_DEMO",
                Map.of("pageUrl", "https://example.test/pricing"),
                NOW);
    }

    private static Map<String, Object> readMap(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private record Harness(
            ConversationWorkspaceService service,
            ConversationSessionMapper sessionMapper,
            ConversationMessageMapper messageMapper,
            ConversationContactProfileMapper contactProfileMapper,
            ConversationWorkItemMapper workItemMapper,
            ConversationSopTaskMapper taskMapper,
            ConversationWorkItemAuditMapper auditMapper) {
    }
}
