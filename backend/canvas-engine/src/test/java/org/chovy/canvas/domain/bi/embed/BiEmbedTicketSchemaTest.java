package org.chovy.canvas.domain.bi.embed;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BiEmbedTicketSchemaTest {

    @Test
    void migrationAddsPersistentConsumptionMetadata() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V312__bi_embed_ticket_consumption.sql"));

        assertThat(sql)
                .contains("ALTER TABLE bi_embed_token ADD COLUMN resource_key")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN nonce")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN consumed_at")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN consumed_origin")
                .contains("idx_bi_embed_token_nonce")
                .contains("idx_bi_embed_token_resource_key");
    }

    @Test
    void migrationAddsAccessLimitMetadata() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V334__bi_embed_access_limits.sql"));

        assertThat(sql)
                .contains("ALTER TABLE bi_embed_token ADD COLUMN access_count")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN max_access_count")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN rate_limit_per_minute")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN rate_window_started_at")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN rate_window_count")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN last_accessed_at")
                .contains("ALTER TABLE bi_embed_token ADD COLUMN last_access_origin")
                .contains("idx_bi_embed_token_access_limit");
    }
}
