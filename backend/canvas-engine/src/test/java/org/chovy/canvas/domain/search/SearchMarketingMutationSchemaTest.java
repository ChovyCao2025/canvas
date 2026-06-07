package org.chovy.canvas.domain.search;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMarketingMutationSchemaTest {

    @Test
    void migrationCreatesMutationLedgerWithIdempotencyAndExecutionIndexes() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V338__search_marketing_mutation_gateway.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_mutation`");
        assertThat(sql).contains("`source_id` BIGINT NOT NULL");
        assertThat(sql).contains("`mutation_key` VARCHAR(128) NOT NULL");
        assertThat(sql).contains("`request_hash` CHAR(64) NOT NULL");
        assertThat(sql).contains("`approval_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
        assertThat(sql).contains("`dry_run_required` TINYINT NOT NULL DEFAULT 1");
        assertThat(sql).contains("`payload_json` JSON NOT NULL");
        assertThat(sql).contains("`provider_response_json` JSON NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_mutation_key` (`tenant_id`, `mutation_key`)");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_mutation_idempotency` (`tenant_id`, `source_id`, `idempotency_key`)");
        assertThat(sql).contains("KEY `idx_search_marketing_mutation_status` (`tenant_id`, `status`, `approval_status`, `updated_at`)");
        assertThat(sql).contains("KEY `idx_search_marketing_mutation_source` (`tenant_id`, `source_id`, `updated_at`)");
    }
}
