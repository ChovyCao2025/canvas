package org.chovy.canvas.domain.ai;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChurnPredictionSmartTimingSchemaTest {

    @Test
    void migrationCreatesRunAndSnapshotTablesWithTenantScopedIndexes() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V165__churn_prediction_smart_timing.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS ai_prediction_run")
                .contains("CREATE TABLE IF NOT EXISTS ai_user_prediction_snapshot")
                .contains("uk_ai_prediction_run")
                .contains("uk_ai_user_prediction_snapshot")
                .contains("idx_ai_prediction_band")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("churn_probability DECIMAL(6,5)")
                .contains("best_send_hour TINYINT")
                .contains("feature_json JSON NOT NULL")
                .contains("contribution_json JSON NOT NULL");
    }
}
