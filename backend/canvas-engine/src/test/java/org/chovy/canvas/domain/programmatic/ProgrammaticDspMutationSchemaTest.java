package org.chovy.canvas.domain.programmatic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProgrammaticDspMutationSchemaTest {

    @Test
    void migrationCreatesProgrammaticDspMutationLedger() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V340__programmatic_dsp_mutation_gateway.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `programmatic_dsp_mutation`");
        assertThat(sql).contains("`seat_id` BIGINT NOT NULL");
        assertThat(sql).contains("`campaign_id` BIGINT NULL");
        assertThat(sql).contains("`line_item_id` BIGINT NULL");
        assertThat(sql).contains("`supply_path_id` BIGINT NULL");
        assertThat(sql).contains("`mutation_key` VARCHAR(128) NOT NULL");
        assertThat(sql).contains("`request_hash` CHAR(64) NOT NULL");
        assertThat(sql).contains("`approval_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
        assertThat(sql).contains("`dry_run_required` TINYINT NOT NULL DEFAULT 1");
        assertThat(sql).contains("`payload_json` JSON NOT NULL");
        assertThat(sql).contains("`provider_response_json` JSON NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_mutation_key` (`tenant_id`, `mutation_key`)");
        assertThat(sql).contains("UNIQUE KEY `uk_programmatic_dsp_mutation_idempotency` (`tenant_id`, `seat_id`, `idempotency_key`)");
        assertThat(sql).contains("KEY `idx_programmatic_dsp_mutation_status` (`tenant_id`, `status`, `approval_status`, `updated_at`)");
        assertThat(sql).contains("KEY `idx_programmatic_dsp_mutation_seat` (`tenant_id`, `seat_id`, `updated_at`)");
        assertThat(sql).contains("KEY `idx_programmatic_dsp_mutation_line_item` (`tenant_id`, `line_item_id`, `updated_at`)");
    }
}
