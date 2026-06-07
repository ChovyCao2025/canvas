package org.chovy.canvas.domain.search;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMarketingProductionClosedLoopSchemaTest {

    @Test
    void migrationCreatesSyncRunUrlInspectionProviderChangeAndImpactTables() throws IOException {
        String sql = migrationSql();

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_sync_run`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_sync_run_idempotency` (`tenant_id`, `source_id`, `run_type`, `idempotency_key`)");
        assertThat(sql).contains("KEY `idx_search_marketing_sync_run_status` (`tenant_id`, `status`, `run_type`, `started_at`)");
        assertThat(sql).contains("KEY `idx_search_marketing_sync_run_source` (`tenant_id`, `source_id`, `status`, `updated_at`)");

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_url_inspection`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_url_inspection_daily` (`tenant_id`, `source_id`, `page_url_hash`, `inspection_date`, `provider`)");
        assertThat(sql).contains("KEY `idx_search_marketing_url_inspection_state` (`tenant_id`, `indexed_state`, `crawl_state`, `inspection_date`)");

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_provider_change`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_provider_change` (`tenant_id`, `source_id`, `provider`, `external_resource_id`, `provider_changed_at`)");
        assertThat(sql).contains("KEY `idx_search_marketing_provider_change_reconcile` (`tenant_id`, `reconciliation_status`, `provider_changed_at`)");

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `search_marketing_impact_window`");
        assertThat(sql).contains("UNIQUE KEY `uk_search_marketing_impact_window` (`tenant_id`, `opportunity_id`, `mutation_id`, `baseline_start_date`, `post_start_date`)");
        assertThat(sql).contains("KEY `idx_search_marketing_impact_window_due` (`tenant_id`, `status`, `due_at`)");
    }

    private String migrationSql() throws IOException {
        try (InputStream stream = getClass()
                .getResourceAsStream("/db/migration/V344__search_marketing_production_closed_loop.sql")) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path path = Path.of("src/main/resources/db/migration/V344__search_marketing_production_closed_loop.sql");
        assertThat(path).as("V344 search marketing closed-loop migration").exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
