package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.chovy.canvas.conversation.domain.ConversationMessage;
import org.chovy.canvas.conversation.domain.ConversationSession;
import org.chovy.canvas.conversation.domain.ConversationWorkItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationPersistenceMappingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 6, 11, 0);

    @Test
    void mapsRepresentativeConversationTablesAndMappersToConversationContext() {
        assertTable(ConversationSessionDO.class, "conversation_session");
        assertTable(ConversationMessageDO.class, "conversation_message");
        assertTable(ConversationContactProfileDO.class, "conversation_contact_profile");
        assertTable(ConversationWorkItemDO.class, "conversation_work_item");
        assertTable(ConversationWorkItemAuditDO.class, "conversation_work_item_audit");
        assertTable(ConversationRoutingAgentDO.class, "conversation_routing_agent");
        assertTable(ConversationRoutingRuleDO.class, "conversation_routing_rule");
        assertTable(ConversationSlaBreachDO.class, "conversation_sla_breach");

        assertThat(BaseMapper.class).isAssignableFrom(ConversationSessionMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationMessageMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationWorkItemMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationWorkItemAuditMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationRoutingAgentMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationRoutingRuleMapper.class);
        assertThat(BaseMapper.class).isAssignableFrom(ConversationSlaBreachMapper.class);
    }

    @Test
    void convertsSessionMessageAndWorkItemRowsWithoutDroppingTenantOrJsonState() {
        ConversationSession session = new ConversationSession(
                1L, 7L, 10L, 20L, "exec-1", "user-1", "WHATSAPP", "TWILIO",
                "ACTIVE", 3, Map.of("intent", "PRODUCT_A"), NOW, NOW.plusDays(1),
                NOW.minusHours(1), NOW);
        ConversationMessage message = new ConversationMessage(
                2L, 7L, 1L, "INBOUND", "TEXT", "msg-1", "WHATSAPP:TWILIO:msg-1",
                Map.of("text", "yes"), "yes", "PRODUCT_A", false, NOW);
        ConversationWorkItem workItem = new ConversationWorkItem(
                3L, 7L, 1L, 4L, "user-1", "WHATSAPP", "TWILIO",
                "WHATSAPP conversation with user-1", "OPEN", "HIGH", "alice", "sales",
                "CONVERSATION", NOW.plusMinutes(30), NOW.plusHours(2), NOW.minusMinutes(5), NOW.minusMinutes(1),
                List.of("vip"), Map.of("segment", "vip"), "ROUTED", List.of("sales", "vip"),
                "matched rule", NOW, "vip-sales", NOW.minusHours(1), NOW);

        ConversationSessionDO sessionRow = ConversationPersistenceConverter.toSessionRow(session);
        ConversationMessageDO messageRow = ConversationPersistenceConverter.toMessageRow(message);
        ConversationWorkItemDO workItemRow = ConversationPersistenceConverter.toWorkItemRow(workItem);

        assertThat(sessionRow.getTenantId()).isEqualTo(7L);
        assertThat(sessionRow.getContextJson()).contains("PRODUCT_A");
        assertThat(messageRow.getIdempotencyKey()).isEqualTo("WHATSAPP:TWILIO:msg-1");
        assertThat(messageRow.getContentJson()).contains("\"text\":\"yes\"");
        assertThat(workItemRow.getRequiredSkillsJson()).contains("sales");
        assertThat(workItemRow.getAttributesJson()).contains("vip");

        assertThat(ConversationPersistenceConverter.toSession(sessionRow).context())
                .containsEntry("intent", "PRODUCT_A");
        assertThat(ConversationPersistenceConverter.toMessage(messageRow).content())
                .containsEntry("text", "yes");
        assertThat(ConversationPersistenceConverter.toWorkItem(workItemRow).requiredSkills())
                .containsExactly("sales", "vip");
    }

    private static void assertTable(Class<?> rowType, String tableName) {
        assertThat(rowType.getAnnotation(TableName.class)).isNotNull();
        assertThat(rowType.getAnnotation(TableName.class).value()).isEqualTo(tableName);
    }
}
