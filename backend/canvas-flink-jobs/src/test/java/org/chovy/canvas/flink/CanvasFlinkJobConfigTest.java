package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Flink 作业环境配置的解析和占位符生成规则。
 */
class CanvasFlinkJobConfigTest {

    /**
     * 缺少 pipeline key 时应拒绝启动配置。
     */
    @Test
    void rejectsMissingPipelineKey() {
        Map<String, String> env = validEnv();
        env.remove("CANVAS_FLINK_JOB_PIPELINE_KEY");

        assertThatThrownBy(() -> CanvasFlinkJobConfig.from(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANVAS_FLINK_JOB_PIPELINE_KEY");
    }

    /**
     * MySQL CDC 管道缺少 MySQL 连接串时应拒绝启动配置。
     */
    @Test
    void rejectsMissingMysqlConnectionForMysqlSourcePipeline() {
        Map<String, String> env = validEnv();
        env.remove("CANVAS_FLINK_MYSQL_URL");

        assertThatThrownBy(() -> CanvasFlinkJobConfig.from(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANVAS_FLINK_MYSQL_URL");
    }

    /**
     * Doris 内部加工管道不应强制要求 MySQL 连接信息。
     */
    @Test
    void doesNotRequireMysqlConnectionForDorisOnlyPipeline() {
        Map<String, String> env = validEnv();
        env.put("CANVAS_FLINK_JOB_PIPELINE_KEY", "doris_ods_cdp_event_to_dwd_fact");
        env.remove("CANVAS_FLINK_MYSQL_URL");
        env.remove("CANVAS_FLINK_MYSQL_USERNAME");
        env.remove("CANVAS_FLINK_MYSQL_PASSWORD");

        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(env);

        assertThat(config.pipelineKey()).isEqualTo("doris_ods_cdp_event_to_dwd_fact");
    }

    /**
     * 缺少 Doris 节点配置时应拒绝启动配置。
     */
    @Test
    void rejectsMissingDorisEndpoints() {
        Map<String, String> env = validEnv();
        env.remove("CANVAS_FLINK_DORIS_FE_NODES");

        assertThatThrownBy(() -> CanvasFlinkJobConfig.from(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANVAS_FLINK_DORIS_FE_NODES");
    }

    /**
     * 缺少 checkpoint 上报地址时应拒绝启动配置。
     */
    @Test
    void rejectsMissingCheckpointEndpoint() {
        Map<String, String> env = validEnv();
        env.remove("CANVAS_FLINK_CHECKPOINT_ENDPOINT");

        assertThatThrownBy(() -> CanvasFlinkJobConfig.from(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANVAS_FLINK_CHECKPOINT_ENDPOINT");
    }

    /**
     * 有效配置应暴露 SQL 渲染需要的全部占位符。
     */
    @Test
    void validConfigExposesPlaceholderValuesForSqlRendering() {
        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(validEnv());

        assertThat(config.tenantId()).isEqualTo(42L);
        assertThat(config.pipelineKey()).isEqualTo("mysql_cdp_event_log_to_doris_ods");
        assertThat(config.checkpointEndpoint())
                .isEqualTo("http://localhost:8080/warehouse/realtime/pipelines/checkpoints");
        assertThat(config.internalApiToken()).isEqualTo("checkpoint-token");
        assertThat(config.dorisLabelSuffix()).isEmpty();
        assertThat(config.sourceSchemaVersion()).isEmpty();
        assertThat(config.sinkSchemaVersion()).isEmpty();
        assertThat(config.placeholders())
                .containsEntry("PIPELINE_KEY", "mysql_cdp_event_log_to_doris_ods")
                .containsEntry("TENANT_ID", "42")
                .containsEntry("MYSQL_HOSTNAME", "mysql")
                .containsEntry("MYSQL_PORT", "3306")
                .containsEntry("MYSQL_DATABASE", "canvas_db")
                .containsEntry("MYSQL_USERNAME", "canvas")
                .containsEntry("MYSQL_PASSWORD", "secret")
                .containsEntry("DORIS_FE_NODES", "doris-fe:8030")
                .containsEntry("DORIS_BE_NODES", "doris-be:8040")
                .containsEntry("DORIS_JDBC_URL", "jdbc:mysql://doris-fe:9030")
                .containsEntry("DORIS_USERNAME", "root")
                .containsEntry("DORIS_PASSWORD", "")
                .containsEntry("DORIS_ODS_DATABASE", "canvas_ods")
                .containsEntry("DORIS_DWD_DATABASE", "canvas_dwd")
                .containsEntry("DORIS_DWS_DATABASE", "canvas_dws")
                .containsEntry("DORIS_LABEL_SUFFIX", "");
    }

    /**
     * schema 版本是可选运行证据，配置后应保留原值。
     */
    @Test
    void schemaVersionsAreOptionalRuntimeEvidence() {
        Map<String, String> env = validEnv();
        env.put("CANVAS_FLINK_SOURCE_SCHEMA_VERSION", "source-v1");
        env.put("CANVAS_FLINK_SINK_SCHEMA_VERSION", "sink-v1");

        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(env);

        assertThat(config.sourceSchemaVersion()).isEqualTo("source-v1");
        assertThat(config.sinkSchemaVersion()).isEqualTo("sink-v1");
    }

    /**
     * Doris label 后缀可选且允许安全字符。
     */
    @Test
    void dorisLabelSuffixIsOptionalAndSanitized() {
        Map<String, String> env = validEnv();
        env.put("CANVAS_FLINK_DORIS_LABEL_SUFFIX", "_live_20260606_123");

        CanvasFlinkJobConfig config = CanvasFlinkJobConfig.from(env);

        assertThat(config.dorisLabelSuffix()).isEqualTo("_live_20260606_123");
        assertThat(config.placeholders()).containsEntry("DORIS_LABEL_SUFFIX", "_live_20260606_123");
    }

    /**
     * Doris label 后缀包含 SQL 危险字符时应拒绝。
     */
    @Test
    void rejectsUnsafeDorisLabelSuffix() {
        Map<String, String> env = validEnv();
        env.put("CANVAS_FLINK_DORIS_LABEL_SUFFIX", "';DROP");

        assertThatThrownBy(() -> CanvasFlinkJobConfig.from(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANVAS_FLINK_DORIS_LABEL_SUFFIX");
    }

    /**
     * 构造覆盖必填项的有效环境变量。
     *
     * @return 可用于大多数配置用例的环境变量映射
     */
    private Map<String, String> validEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("CANVAS_FLINK_JOB_PIPELINE_KEY", "mysql_cdp_event_log_to_doris_ods");
        env.put("CANVAS_FLINK_TENANT_ID", "42");
        env.put("CANVAS_FLINK_MYSQL_URL", "jdbc:mysql://mysql:3306/canvas_db?useSSL=false");
        env.put("CANVAS_FLINK_MYSQL_USERNAME", "canvas");
        env.put("CANVAS_FLINK_MYSQL_PASSWORD", "secret");
        env.put("CANVAS_FLINK_DORIS_FE_NODES", "doris-fe:8030");
        env.put("CANVAS_FLINK_DORIS_BE_NODES", "doris-be:8040");
        env.put("CANVAS_FLINK_DORIS_JDBC_URL", "jdbc:mysql://doris-fe:9030");
        env.put("CANVAS_FLINK_DORIS_USERNAME", "root");
        env.put("CANVAS_FLINK_DORIS_PASSWORD", "");
        env.put("CANVAS_FLINK_CHECKPOINT_ENDPOINT",
                "http://localhost:8080/warehouse/realtime/pipelines/checkpoints");
        env.put("CANVAS_FLINK_INTERNAL_API_TOKEN", "checkpoint-token");
        return env;
    }
}
