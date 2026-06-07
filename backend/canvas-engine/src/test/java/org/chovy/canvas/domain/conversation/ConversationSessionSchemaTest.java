package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationSessionSchemaTest {

    @Test
    void migrationCreatesConversationSessionAndMessageTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V270__conversation_session_foundation.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS conversation_session")
                .contains("CREATE TABLE IF NOT EXISTS conversation_message")
                .contains("uk_conversation_message_idempotency")
                .contains("idx_conversation_session_active")
                .contains("idx_conversation_message_session");
    }
}
