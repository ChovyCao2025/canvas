package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationPrivateDomainSchemaTest {

    @Test
    void migrationAddsPrivateDomainContactGroupAndSyncRunTables() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V307__private_domain_contact_group_sync.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_private_contact");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_private_contact_owner");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_private_group");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_private_group_member");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_private_sync_run");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_private_contact");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_private_contact_owner");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_private_group");
        assertThat(sql).contains("UNIQUE KEY uk_conversation_private_group_member");
        assertThat(sql).contains("INDEX idx_conversation_private_sync_run_provider");
    }
}
