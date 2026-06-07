package org.chovy.canvas.domain.paidmedia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PaidMediaAudienceSyncSchemaTest {

    @Test
    void migrationCreatesPaidMediaAudienceSyncTablesAndIndexes() throws IOException {
        String sql = new String(getClass().getResourceAsStream(
                "/db/migration/V308__paid_media_audience_sync.sql").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("paid_media_audience_destination");
        assertThat(sql).contains("paid_media_audience_member");
        assertThat(sql).contains("paid_media_audience_sync_run");
        assertThat(sql).contains("uk_paid_media_destination");
        assertThat(sql).contains("idx_paid_media_member_run_status");
        assertThat(sql).contains("idx_paid_media_sync_run_destination");
    }
}
