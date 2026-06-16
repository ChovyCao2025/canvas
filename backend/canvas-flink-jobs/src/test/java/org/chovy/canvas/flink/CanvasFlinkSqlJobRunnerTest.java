package org.chovy.canvas.flink;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Flink SQL 脚本拆分和执行器提交行为。
 */
class CanvasFlinkSqlJobRunnerTest {

    /**
     * 多条 SQL 应去除首尾空白后按顺序提交并返回数量。
     */
    @Test
    void runExecutesTrimmedStatementsInOrderAndReturnsCount() {
        List<String> executed = new ArrayList<>();

        int statementCount = CanvasFlinkSqlJobRunner.run("""
                  CREATE TABLE source_table (id BIGINT);

                  INSERT INTO sink_table SELECT * FROM source_table;
                """, executed::add);

        assertThat(statementCount).isEqualTo(2);
        assertThat(executed).containsExactly(
                "CREATE TABLE source_table (id BIGINT)",
                "INSERT INTO sink_table SELECT * FROM source_table");
    }

    /**
     * 空脚本或空语句不应提交给执行器。
     */
    @Test
    void runSkipsNullBlankAndEmptyStatements() {
        List<String> executed = new ArrayList<>();

        assertThat(CanvasFlinkSqlJobRunner.run(null, executed::add)).isZero();
        assertThat(CanvasFlinkSqlJobRunner.run("   \n\t  ", executed::add)).isZero();
        assertThat(CanvasFlinkSqlJobRunner.run(" ; ; \n ; ", executed::add)).isZero();
        assertThat(executed).isEmpty();
    }

    /**
     * 缺少执行器时应拒绝运行 SQL。
     */
    @Test
    void runRejectsMissingExecutor() {
        assertThatThrownBy(() -> CanvasFlinkSqlJobRunner.run("SELECT 1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL executor is required");
    }

    /**
     * 单条语句执行失败时应停止后续提交并向上传播异常。
     */
    @Test
    void runPropagatesExecutionFailureAndStopsSubmittingRemainingStatements() {
        List<String> executed = new ArrayList<>();

        assertThatThrownBy(() -> CanvasFlinkSqlJobRunner.run("""
                CREATE TABLE source_table (id BIGINT);
                INSERT INTO sink_table SELECT * FROM source_table;
                SELECT * FROM sink_table;
                """, statement -> {
            executed.add(statement);
            if (statement.startsWith("INSERT INTO")) {
                throw new IllegalStateException("flink rejected SQL");
            }
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("flink rejected SQL");

        assertThat(executed).containsExactly(
                "CREATE TABLE source_table (id BIGINT)",
                "INSERT INTO sink_table SELECT * FROM source_table");
    }

    /**
     * 字符串字面量内部的分号不应切分语句。
     */
    @Test
    void statementsDoNotSplitSemicolonsInsideStringLiterals() {
        assertThat(CanvasFlinkSqlJobRunner.statements("""
                CREATE TABLE source_table (note STRING);
                INSERT INTO source_table VALUES ('a;b', 'it''s still one; value');
                """))
                .containsExactly(
                        "CREATE TABLE source_table (note STRING)",
                        "INSERT INTO source_table VALUES ('a;b', 'it''s still one; value')");
    }

    /**
     * SQL 注释内部的分号不应切分语句。
     */
    @Test
    void statementsDoNotSplitSemicolonsInsideSqlComments() {
        assertThat(CanvasFlinkSqlJobRunner.statements("""
                -- comment includes ; but is not a delimiter
                CREATE TABLE source_table (id BIGINT);
                /* block comment includes ;
                   across lines */
                INSERT INTO sink_table SELECT * FROM source_table;
                """))
                .containsExactly(
                        "-- comment includes ; but is not a delimiter\nCREATE TABLE source_table (id BIGINT)",
                        "/* block comment includes ;\n   across lines */\nINSERT INTO sink_table SELECT * FROM source_table");
    }

    /**
     * dollar quote 代码块内部的分号不应切分语句。
     */
    @Test
    void statementsDoNotSplitSemicolonsInsideDollarQuotedBlocks() {
        assertThat(CanvasFlinkSqlJobRunner.statements("""
                CREATE TEMPORARY FUNCTION normalize_note AS $$
                  SELECT 'a;b';
                  SELECT 'still inside dollar quote';
                $$;
                INSERT INTO sink_table SELECT $body$literal;content$body$ FROM source_table;
                """))
                .containsExactly(
                        """
                        CREATE TEMPORARY FUNCTION normalize_note AS $$
                          SELECT 'a;b';
                          SELECT 'still inside dollar quote';
                        $$""".stripTrailing(),
                        "INSERT INTO sink_table SELECT $body$literal;content$body$ FROM source_table");
    }
}
