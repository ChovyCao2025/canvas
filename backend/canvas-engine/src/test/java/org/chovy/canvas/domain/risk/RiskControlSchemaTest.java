package org.chovy.canvas.domain.risk;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RiskControlSchemaTest {

    @Test
    void migrationDefinesRiskControlFoundationTables() throws Exception {
        InputStream resource = getClass()
                .getResourceAsStream("/db/migration/V357__risk_control_rule_engine_foundation.sql");
        assertThat(resource).isNotNull();

        String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS risk_scene")
                .contains("CREATE TABLE IF NOT EXISTS risk_strategy")
                .contains("CREATE TABLE IF NOT EXISTS risk_strategy_version")
                .contains("CREATE TABLE IF NOT EXISTS risk_list")
                .contains("CREATE TABLE IF NOT EXISTS risk_list_entry")
                .contains("CREATE TABLE IF NOT EXISTS risk_decision_run")
                .contains("CREATE TABLE IF NOT EXISTS risk_rule_hit")
                .contains("tenant_id")
                .contains("uk_risk_scene_tenant_key")
                .contains("uk_risk_strategy_tenant_key")
                .contains("uk_risk_strategy_version")
                .contains("uk_risk_list_entry_subject")
                .contains("idx_risk_decision_scene_time")
                .contains("idx_risk_decision_subject_time");
    }

    @Test
    void followupMigrationPersistsStrategyVersionLifecycle() throws Exception {
        InputStream resource = getClass()
                .getResourceAsStream("/db/migration/V358__risk_governance_persistence_closure.sql");
        assertThat(resource).isNotNull();

        String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE risk_strategy_version")
                .contains("ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'")
                .contains("ADD COLUMN submitted_by VARCHAR(128) NULL")
                .contains("ADD COLUMN submitted_at DATETIME(3) NULL")
                .contains("idx_risk_strategy_version_status");
    }

    @Test
    void simulationMigrationPersistsRiskLabRuns() throws Exception {
        InputStream resource = getClass()
                .getResourceAsStream("/db/migration/V359__risk_simulation_run_history.sql");
        assertThat(resource).isNotNull();

        String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS risk_simulation_run")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("simulation_id VARCHAR(128) NOT NULL")
                .contains("action_distribution_json JSON NOT NULL")
                .contains("uk_risk_simulation_run")
                .contains("idx_risk_simulation_scene_time");
    }
}
