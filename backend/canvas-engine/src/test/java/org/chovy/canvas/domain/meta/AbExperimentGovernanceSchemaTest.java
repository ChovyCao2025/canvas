package org.chovy.canvas.domain.meta;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AbExperimentGovernanceSchemaTest {

    @Test
    void migrationAddsExperimentMetricsGovernanceAndAllocationContracts() throws IOException {
        InputStream migration = getClass().getResourceAsStream(
                "/db/migration/V276__ab_experiment_metrics_governance.sql");

        assertThat(migration).isNotNull();
        String sql = new String(migration.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ab_experiment_layer`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ab_experiment_allocation`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ab_experiment_metric`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ab_experiment_metric_snapshot`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `ab_experiment_governance_decision`");
        assertThat(sql).contains("UNIQUE KEY `uk_ab_experiment_metric` (`experiment_id`, `metric_key`)");
        assertThat(sql).contains("KEY `idx_ab_experiment_metric_role` (`experiment_id`, `metric_role`, `enabled`)");
        assertThat(sql).contains("UNIQUE KEY `uk_ab_experiment_snapshot` (`experiment_id`, `variant_key`, `metric_key`, `observed_at`)");
        assertThat(sql).contains("`winner_variant_key` VARCHAR(64) NULL");
        assertThat(sql).contains("`writeback_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_READY'");
    }
}
