package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedProfileAttributeSchemaTest {

    @Test
    void migrationCreatesComputedProfileTables() throws Exception {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/V104__cdp_computed_profile_attributes.sql"))) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE IF NOT EXISTS cdp_computed_profile_attribute")
                    .contains("attr_code")
                    .contains("compute_type")
                    .contains("refresh_mode")
                    .contains("CREATE TABLE IF NOT EXISTS cdp_computed_profile_run")
                    .contains("source_event_id")
                    .contains("scanned_count")
                    .contains("changed_count")
                    .contains("CREATE TABLE IF NOT EXISTS cdp_profile_attribute_change_log")
                    .contains("old_value")
                    .contains("new_value")
                    .contains("source_run_id");
        }
    }
}
