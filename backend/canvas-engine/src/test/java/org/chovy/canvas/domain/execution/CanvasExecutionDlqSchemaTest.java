package org.chovy.canvas.domain.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasExecutionDlqSchemaTest {

    @Test
    void dlqMigrationIncludesTriggerReplayContextColumns() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V52__dlq_trigger_context.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("`trigger_type`")
                .contains("`trigger_node_type`")
                .contains("`match_key`");
    }
}
