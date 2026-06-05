package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EventAttributeDiscoverySchemaTest {

    @Test
    void migrationAddsDiscoveryControlsAndAttributeTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql"));

        assertThat(sql)
                .contains("ALTER TABLE event_definition")
                .contains("auto_discover")
                .contains("discovery_mode")
                .contains("CREATE TABLE IF NOT EXISTS event_attr_definition")
                .contains("attr_type")
                .contains("PENDING_REVIEW")
                .contains("UNIQUE KEY uk_event_attr_definition");
    }

    @Test
    void applicationConfigDeclaresInternalCdpEventTopic() throws Exception {
        String yml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(yml)
                .contains("cdp:")
                .contains("event-topic: ${CANVAS_CDP_EVENT_TOPIC:CDP_EVENT_INGESTED}");
    }
}
