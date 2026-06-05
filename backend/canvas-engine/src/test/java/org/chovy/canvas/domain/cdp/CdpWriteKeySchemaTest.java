package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CdpWriteKeySchemaTest {

    @Test
    void migrationCreatesWriteKeyTableWithoutRawSecretStorage() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V100__cdp_write_key_management.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_write_key")
                .contains("tenant_id")
                .contains("key_prefix")
                .contains("key_hash")
                .contains("platform")
                .contains("rate_limit_qps")
                .contains("daily_quota")
                .contains("UNIQUE KEY uk_cdp_write_key_prefix")
                .doesNotContain("raw_key");
    }
}
