package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasFlinkSqlTemplateLoaderTest {

    @Test
    void loadsClasspathSqlAsset() {
        String sql = CanvasFlinkSqlTemplateLoader.load("sql/mysql_cdp_event_log_to_doris_ods.sql");

        assertThat(sql)
                .contains("mysql_cdp_event_log_to_doris_ods")
                .contains("CREATE TABLE mysql_cdp_event_log_source");
    }

    @Test
    void renderReplacesPlaceholderTokens() {
        String rendered = CanvasFlinkSqlTemplateLoader.render(
                "connector=${CONNECTOR}; nodes=${DORIS_FE_NODES};",
                Map.of("CONNECTOR", "doris", "DORIS_FE_NODES", "doris-fe:8030"));

        assertThat(rendered).isEqualTo("connector=doris; nodes=doris-fe:8030;");
    }

    @Test
    void renderRejectsMissingPlaceholderValue() {
        assertThatThrownBy(() -> CanvasFlinkSqlTemplateLoader.render(
                "nodes=${DORIS_FE_NODES}; password=${DORIS_PASSWORD};",
                Map.of("DORIS_FE_NODES", "doris-fe:8030")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DORIS_PASSWORD");
    }

    @Test
    void renderLeavesNoPlaceholderTokenInFinalSql() {
        String template = CanvasFlinkSqlTemplateLoader.load("sql/doris_ods_cdp_event_to_dwd_fact.sql");
        String rendered = CanvasFlinkSqlTemplateLoader.render(template, validPlaceholders());

        assertThat(rendered)
                .doesNotContain("${")
                .contains("'fenodes' = 'doris-fe:8030'")
                .contains("'jdbc-url' = 'jdbc:mysql://doris-fe:9030'")
                .contains("'sink.label-prefix' = 'doris_ods_cdp_event_to_dwd_fact_42'");
    }

    private Map<String, String> validPlaceholders() {
        return CanvasFlinkJobConfig.from(Map.of(
                "CANVAS_FLINK_JOB_PIPELINE_KEY", "doris_ods_cdp_event_to_dwd_fact",
                "CANVAS_FLINK_TENANT_ID", "42",
                "CANVAS_FLINK_DORIS_FE_NODES", "doris-fe:8030",
                "CANVAS_FLINK_DORIS_BE_NODES", "doris-be:8040",
                "CANVAS_FLINK_DORIS_JDBC_URL", "jdbc:mysql://doris-fe:9030",
                "CANVAS_FLINK_DORIS_USERNAME", "root",
                "CANVAS_FLINK_CHECKPOINT_ENDPOINT",
                "http://localhost:8080/warehouse/realtime/pipelines/checkpoints"
        )).placeholders();
    }
}
