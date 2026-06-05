package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelConnectorSchemaTest {

    @Test
    void migrationCreatesConnectorTable() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V120__channel_connector_contract.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS channel_connector")
                .contains("connector_key")
                .contains("channel")
                .contains("provider")
                .contains("mode")
                .contains("capabilities_json")
                .contains("health_status")
                .contains("disabled_reason")
                .contains("UNIQUE KEY uk_channel_connector");
    }
}
