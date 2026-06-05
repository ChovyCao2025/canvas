package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelProviderPolicySchemaTest {

    @Test
    void migrationCreatesProviderPolicyTables() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V121__channel_provider_policies.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS channel_provider_limit")
                .contains("per_second_limit")
                .contains("daily_limit")
                .contains("CREATE TABLE IF NOT EXISTS channel_fallback_policy")
                .contains("fallback_channel")
                .contains("fallback_provider")
                .contains("CREATE TABLE IF NOT EXISTS channel_fallback_decision")
                .contains("attempt_chain_json")
                .contains("CREATE TABLE IF NOT EXISTS channel_dedupe_record")
                .contains("dedupe_group")
                .contains("content_hash");
    }
}
