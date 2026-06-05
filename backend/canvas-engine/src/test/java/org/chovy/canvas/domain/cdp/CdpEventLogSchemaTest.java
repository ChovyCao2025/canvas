package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CdpEventLogSchemaTest {

    @Test
    void migrationCreatesEnrichedEventLogWithDuplicateKeys() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_event_log")
                .contains("write_key_id")
                .contains("message_id")
                .contains("anonymous_id")
                .contains("session_id")
                .contains("device_id")
                .contains("sdk_context")
                .contains("properties")
                .contains("UNIQUE KEY uk_cdp_event_message")
                .contains("UNIQUE KEY uk_cdp_event_idempotency");
    }

    @Test
    void applicationConfigDeclaresCdpIngestionBatchLimit() throws Exception {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(yml)
                .contains("cdp:")
                .contains("ingestion:")
                .contains("max-batch-size: ${CANVAS_CDP_INGESTION_MAX_BATCH_SIZE:100}");
    }
}
