package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasFlinkJobMainTest {

    @Test
    void runSelectsPipelineKeyRendersSqlExecutesStatementsAndReportsPass() {
        List<String> statements = new ArrayList<>();
        List<CanvasFlinkCheckpointReporter.CheckpointPayload> reports = new ArrayList<>();

        CanvasFlinkJobMain.RunResult result = CanvasFlinkJobMain.run(
                validEnv(),
                statements::add,
                reports::add,
                () -> "2026-06-06T10:00:00");

        assertThat(result.pipelineKey()).isEqualTo("mysql_cdp_event_log_to_doris_ods");
        assertThat(result.statementCount()).isEqualTo(3);
        assertThat(statements).hasSize(3);
        assertThat(statements).allSatisfy(statement -> assertThat(statement).doesNotContain("${"));
        assertThat(statements.get(0))
                .contains("'connector' = 'mysql-cdc'")
                .contains("'hostname' = 'mysql'");
        assertThat(statements.get(1))
                .contains("'connector' = 'doris'")
                .contains("'table.identifier' = 'canvas_ods.cdp_event_log'");
        assertThat(statements.get(2)).startsWith("INSERT INTO doris_cdp_event_log_ods_sink");
        assertThat(reports)
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.pipelineKey()).isEqualTo("mysql_cdp_event_log_to_doris_ods");
                    assertThat(report.status()).isEqualTo("WARN");
                    assertThat(report.errorMessage()).isEqualTo("startup submission is not runtime checkpoint evidence");
                    assertThat(report.checkpointId()).isEqualTo("mysql_cdp_event_log_to_doris_ods-startup");
                    assertThat(report.checkpointTime()).isEqualTo("2026-06-06T10:00:00");
                    assertThat(report.reportedBy()).isEqualTo("canvas-flink-jobs");
                    assertThat(report.sourceSchemaVersion()).isNull();
                    assertThat(report.sinkSchemaVersion()).isNull();
                });
    }

    @Test
    void runReportsFailWhenSqlExecutionFails() {
        List<CanvasFlinkCheckpointReporter.CheckpointPayload> reports = new ArrayList<>();

        assertThatThrownBy(() -> CanvasFlinkJobMain.run(
                validEnv(),
                statement -> {
                    throw new IllegalStateException("flink rejected SQL");
                },
                reports::add,
                () -> "2026-06-06T10:00:00"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("flink rejected SQL");

        assertThat(reports)
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.status()).isEqualTo("FAIL");
                    assertThat(report.errorMessage()).contains("flink rejected SQL");
                });
    }

    @Test
    void rejectsUnknownPipelineKeyBeforeSubmittingSql() {
        Map<String, String> env = validEnv();
        env.put("CANVAS_FLINK_JOB_PIPELINE_KEY", "unknown");
        List<String> statements = new ArrayList<>();

        assertThatThrownBy(() -> CanvasFlinkJobMain.run(
                env,
                statements::add,
                payload -> {
                },
                () -> "2026-06-06T10:00:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown pipeline");
        assertThat(statements).isEmpty();
    }

    @Test
    void runnerSplitsSemicolonDelimitedSqlWithoutSplittingQuotedSemicolon() {
        assertThat(CanvasFlinkSqlJobRunner.statements("""
                CREATE TABLE t (value STRING, note STRING);
                INSERT INTO t SELECT 'a;b', 'quoted semicolon';
                """))
                .containsExactly(
                        "CREATE TABLE t (value STRING, note STRING)",
                        "INSERT INTO t SELECT 'a;b', 'quoted semicolon'");
    }

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
        return env;
    }
}
