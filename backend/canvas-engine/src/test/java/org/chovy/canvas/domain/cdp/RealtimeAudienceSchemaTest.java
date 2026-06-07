package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeAudienceSchemaTest {

    @Test
    void migrationCreatesRealtimeEventLogAndSnapshotTables() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V106__realtime_audience_overlap_snapshots.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS cdp_realtime_audience_event_log")
                    .contains("tenant_id")
                    .contains("audience_id")
                    .contains("source_event_id")
                    .contains("operation")
                    .contains("UNIQUE KEY uk_cdp_realtime_audience_event")
                    .contains("CREATE TABLE IF NOT EXISTS cdp_audience_snapshot")
                    .contains("estimated_size")
                    .contains("bitmap_key")
                    .contains("snapshot_source")
                    .contains("created_at");
        }
    }
}
