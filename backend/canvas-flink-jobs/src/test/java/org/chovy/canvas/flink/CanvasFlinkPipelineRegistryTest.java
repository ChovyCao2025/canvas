package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasFlinkPipelineRegistryTest {

    @Test
    void resolvesAllFirstSlicePipelineKeysToSqlAssets() {
        assertThat(CanvasFlinkPipelineRegistry.all())
                .containsEntry("mysql_cdp_event_log_to_doris_ods",
                        "sql/mysql_cdp_event_log_to_doris_ods.sql")
                .containsEntry("mysql_canvas_trace_to_doris_ods",
                        "sql/mysql_canvas_trace_to_doris_ods.sql")
                .containsEntry("doris_ods_cdp_event_to_dwd_fact",
                        "sql/doris_ods_cdp_event_to_dwd_fact.sql")
                .containsEntry("doris_dwd_user_fact_to_dws_metric_daily",
                        "sql/doris_dwd_user_fact_to_dws_metric_daily.sql")
                .containsEntry("risk_realtime_features",
                        "sql/risk_realtime_features.sql")
                .hasSize(5);
    }

    @Test
    void rejectsUnknownPipelineKey() {
        assertThatThrownBy(() -> CanvasFlinkPipelineRegistry.sqlAssetFor("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown pipeline");
    }

    @Test
    void sqlAssetsContainExpectedSinkTablesAndPipelineKeys() throws Exception {
        Map<String, String> expectedSinkTables = Map.of(
                "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log",
                "mysql_canvas_trace_to_doris_ods", "canvas_ods.canvas_execution_trace",
                "doris_ods_cdp_event_to_dwd_fact", "canvas_dwd.cdp_user_event_fact",
                "doris_dwd_user_fact_to_dws_metric_daily", "canvas_dws.user_event_metric_daily",
                "risk_realtime_features", "canvas_dws.risk_realtime_feature_snapshot");

        for (Map.Entry<String, String> entry : CanvasFlinkPipelineRegistry.all().entrySet()) {
            String sql = readResource(entry.getValue());

            assertThat(sql)
                    .contains(entry.getKey())
                    .contains(expectedSinkTables.get(entry.getKey()))
                    .contains("${DORIS_FE_NODES}")
                    .contains("${DORIS_JDBC_URL}");
        }
    }

    private String readResource(String assetPath) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(assetPath)) {
            assertThat(stream).as(assetPath).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
