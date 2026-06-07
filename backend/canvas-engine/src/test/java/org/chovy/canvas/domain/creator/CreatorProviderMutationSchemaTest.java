package org.chovy.canvas.domain.creator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CreatorProviderMutationSchemaTest {

    @Test
    void migrationCreatesCreatorProviderMutationLedger() throws IOException {
        String sql = new String(getClass()
                .getResourceAsStream("/db/migration/V339__creator_provider_mutation_gateway.sql")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `creator_provider_mutation`");
        assertThat(sql).contains("`campaign_id` BIGINT NOT NULL");
        assertThat(sql).contains("`collaboration_id` BIGINT NULL");
        assertThat(sql).contains("`deliverable_id` BIGINT NULL");
        assertThat(sql).contains("`mutation_key` VARCHAR(128) NOT NULL");
        assertThat(sql).contains("`request_hash` CHAR(64) NOT NULL");
        assertThat(sql).contains("`approval_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING'");
        assertThat(sql).contains("`dry_run_required` TINYINT NOT NULL DEFAULT 1");
        assertThat(sql).contains("`payload_json` JSON NOT NULL");
        assertThat(sql).contains("`provider_response_json` JSON NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_creator_provider_mutation_key` (`tenant_id`, `mutation_key`)");
        assertThat(sql).contains("UNIQUE KEY `uk_creator_provider_mutation_idempotency` (`tenant_id`, `provider`, `idempotency_key`)");
        assertThat(sql).contains("KEY `idx_creator_provider_mutation_status` (`tenant_id`, `status`, `approval_status`, `updated_at`)");
        assertThat(sql).contains("KEY `idx_creator_provider_mutation_campaign` (`tenant_id`, `campaign_id`, `updated_at`)");
    }
}
