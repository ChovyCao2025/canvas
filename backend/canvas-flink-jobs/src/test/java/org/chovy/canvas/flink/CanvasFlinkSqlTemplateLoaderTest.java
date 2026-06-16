package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 SQL 模板加载和占位符渲染规则。
 */
class CanvasFlinkSqlTemplateLoaderTest {

    /**
     * 应能从 classpath 读取已发布的 SQL asset。
     */
    @Test
    void loadsClasspathSqlAsset() {
        String sql = CanvasFlinkSqlTemplateLoader.load("sql/mysql_cdp_event_log_to_doris_ods.sql");

        assertThat(sql)
                .contains("mysql_cdp_event_log_to_doris_ods")
                .contains("CREATE TABLE mysql_cdp_event_log_source");
    }

    /**
     * 渲染时应替换模板中的大写占位符。
     */
    @Test
    void renderReplacesPlaceholderTokens() {
        String rendered = CanvasFlinkSqlTemplateLoader.render(
                "connector=${CONNECTOR}; nodes=${DORIS_FE_NODES};",
                Map.of("CONNECTOR", "doris", "DORIS_FE_NODES", "doris-fe:8030"));

        assertThat(rendered).isEqualTo("connector=doris; nodes=doris-fe:8030;");
    }

    /**
     * 缺少占位符值时应拒绝生成 SQL。
     */
    @Test
    void renderRejectsMissingPlaceholderValue() {
        assertThatThrownBy(() -> CanvasFlinkSqlTemplateLoader.render(
                "nodes=${DORIS_FE_NODES}; password=${DORIS_PASSWORD};",
                Map.of("DORIS_FE_NODES", "doris-fe:8030")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DORIS_PASSWORD");
    }

    /**
     * 完整占位符映射渲染后不应残留 `${...}` 变量。
     */
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

    /**
     * 构造 Doris 派生层 SQL 渲染所需的占位符。
     *
     * @return SQL 占位符映射
     */
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
