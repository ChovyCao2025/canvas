package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationWorkspaceSchemaTest {

    @Test
    void migrationAddsScrmOperatorWorkspaceTablesAndIndexes() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V305__scrm_operator_workspace.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_contact_profile");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_work_item");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_sop_task");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_work_item_audit");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_contact_profile_user");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_work_item_session");
        assertThat(sql).contains("INDEX idx_conversation_work_item_inbox");
        assertThat(sql).contains("INDEX idx_conversation_sop_task_work_item");
        assertThat(sql).contains("INDEX idx_conversation_work_item_audit_item");
    }
}
