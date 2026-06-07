package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;

class ConversationRoutingSchemaTest {

    @Test
    void migrationAddsRoutingAgentsRulesBreachesAndWorkItemRoutingColumns() throws IOException {
        String sql = readMigration("scrm_routing_sla");

        assertThat(sql).contains("ALTER TABLE conversation_work_item");
        assertThat(sql).contains("routing_status VARCHAR(32) NOT NULL DEFAULT 'UNROUTED'");
        assertThat(sql).contains("required_skills_json JSON NULL");
        assertThat(sql).contains("sla_policy_key VARCHAR(128) NULL");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_routing_agent");
        assertThat(sql).contains("agent_key VARCHAR(128) NOT NULL");
        assertThat(sql).contains("skills_json JSON NULL");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_routing_agent");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_routing_rule");
        assertThat(sql).contains("required_skills_json JSON NULL");
        assertThat(sql).contains("idx_conversation_routing_rule_match");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_sla_breach");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_sla_breach_open");
        assertThat(sql).contains("idx_conversation_sla_breach_status");
    }
}
