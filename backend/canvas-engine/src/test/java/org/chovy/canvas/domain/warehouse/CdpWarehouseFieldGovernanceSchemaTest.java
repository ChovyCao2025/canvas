package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.chovy.canvas.testsupport.MigrationTestSupport.readMigration;
import static org.assertj.core.api.Assertions.assertThat;

class CdpWarehouseFieldGovernanceSchemaTest {

    @Test
    void migrationCreatesFieldPoliciesAuditAndBuiltInSeeds() throws Exception {
        String sql = readMigration("cdp_warehouse_field_governance");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_field_policy")
                .contains("CREATE TABLE IF NOT EXISTS cdp_warehouse_field_access_audit")
                .contains("UNIQUE KEY uk_cdp_warehouse_field_policy")
                .contains("INDEX idx_cdp_warehouse_field_policy_dataset")
                .contains("INDEX idx_cdp_warehouse_field_audit_decision")
                .contains("'canvas_daily_stats', 'unique_users'")
                .contains("'cdp_ods_event_log', 'user_id'")
                .contains("'cdp_ods_event_log', 'properties'")
                .contains("'cdp_dwd_user_event_fact', 'properties_json'")
                .contains("'cdp_dws_user_event_metric_daily', 'user_id'")
                .contains("'PII_RELATED', 'MASK', 'TENANT_ADMIN'")
                .contains("'AGGREGATED_USER', 'ALLOW', 'OPERATOR'");
    }
}
