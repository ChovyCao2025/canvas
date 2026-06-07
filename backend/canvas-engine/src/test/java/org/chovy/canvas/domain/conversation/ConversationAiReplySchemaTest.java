package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationAiReplySchemaTest {

    @Test
    void migrationAddsAiReplySuggestionLedger() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V318__scrm_ai_reply_assistance.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS conversation_ai_reply_suggestion");
        assertThat(sql).contains("work_item_id BIGINT NOT NULL");
        assertThat(sql).contains("source_message_id BIGINT NULL");
        assertThat(sql).contains("prompt_context_json JSON NULL");
        assertThat(sql).contains("risk_flags_json JSON NULL");
        assertThat(sql).contains("grounding_snippets_json JSON NULL");
        assertThat(sql).contains("status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'");
        assertThat(sql).contains("INDEX idx_conversation_ai_reply_work_item");
        assertThat(sql).contains("INDEX idx_conversation_ai_reply_status");
        assertThat(sql).contains("INDEX idx_conversation_ai_reply_review");
    }
}
