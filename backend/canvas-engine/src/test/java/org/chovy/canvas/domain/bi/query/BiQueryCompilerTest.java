package org.chovy.canvas.domain.bi.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiQueryCompilerTest {

    private final BiDatasetSpec dataset = new BiDatasetSpec(
            "canvas_daily_stats",
            "canvas_dws.canvas_daily_stats",
            "tenant_id",
            Map.of(
                    "stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE"),
                    "canvas_name", new BiFieldSpec("canvas_name", "canvas_name", BiFieldSpec.Role.DIMENSION, "STRING"),
                    "trigger_type", new BiFieldSpec("trigger_type", "trigger_type", BiFieldSpec.Role.DIMENSION, "STRING"),
                    "total_executions", new BiFieldSpec("total_executions", "total_executions", BiFieldSpec.Role.MEASURE, "NUMBER")
            ),
            Map.of(
                    "total_executions", new BiMetricSpec("total_executions", "SUM(total_executions)", "NUMBER"),
                    "date_only_metric", new BiMetricSpec("date_only_metric", "SUM(total_executions)", "NUMBER",
                            List.of("stat_date")),
                    "success_rate", new BiMetricSpec(
                            "success_rate",
                            "CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END",
                            "PERCENT")
            )
    );

    private final BiQueryCompiler compiler = new BiQueryCompiler();

    @Test
    void compilesGroupedQueryAndInjectsTenantFilter() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date", "canvas_name"),
                List.of("total_executions", "success_rate"),
                List.of(new BiFilter("stat_date", BiFilter.Operator.BETWEEN, List.of("2026-06-01", "2026-06-05"))),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                500
        );

        BiCompiledQuery query = compiler.compile(dataset, request, 7L);

        assertThat(query.sql()).isEqualTo("""
                SELECT stat_date AS stat_date, canvas_name AS canvas_name, SUM(total_executions) AS total_executions, CASE WHEN SUM(total_executions) > 0 THEN SUM(success_count) / SUM(total_executions) ELSE 0 END AS success_rate
                FROM canvas_dws.canvas_daily_stats
                WHERE tenant_id = ? AND stat_date BETWEEN ? AND ?
                GROUP BY stat_date, canvas_name
                ORDER BY stat_date ASC
                LIMIT 500
                """.stripTrailing());
        assertThat(query.parameters()).containsExactly(7L, "2026-06-01", "2026-06-05");
    }

    @Test
    void compilesBoundParametersForSupportedFilters() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("trigger_type"),
                List.of("total_executions"),
                List.of(
                        new BiFilter("trigger_type", BiFilter.Operator.IN, List.of("EVENT", "MQ")),
                        new BiFilter("canvas_name", BiFilter.Operator.CONTAINS, "welcome"),
                        new BiFilter("stat_date", BiFilter.Operator.GTE, "2026-06-01"),
                        new BiFilter("stat_date", BiFilter.Operator.LTE, "2026-06-05")
                ),
                List.of(new BiSort("total_executions", BiSort.Direction.DESC)),
                50
        );

        BiCompiledQuery query = compiler.compile(dataset, request, 7L);

        assertThat(query.sql()).contains("trigger_type IN (?, ?)");
        assertThat(query.sql()).contains("canvas_name LIKE ?");
        assertThat(query.sql()).contains("stat_date >= ?");
        assertThat(query.sql()).contains("stat_date <= ?");
        assertThat(query.sql()).contains("ORDER BY total_executions DESC");
        assertThat(query.parameters()).containsExactly(7L, "EVENT", "MQ", "%welcome%", "2026-06-01", "2026-06-05");
    }

    @Test
    void rejectsUnknownDimensionBeforeSqlGeneration() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("unsafe_field"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100
        );

        assertThatThrownBy(() -> compiler.compile(dataset, request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown dimension");
    }

    @Test
    void rejectsUnknownMetricBeforeSqlGeneration() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("drop_table"),
                List.of(),
                List.of(),
                100
        );

        assertThatThrownBy(() -> compiler.compile(dataset, request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown metric");
    }

    @Test
    void rejectsMetricWhenDimensionIsOutsideMetricContract() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("canvas_name"),
                List.of("date_only_metric"),
                List.of(),
                List.of(),
                100
        );

        assertThatThrownBy(() -> compiler.compile(dataset, request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow dimension: canvas_name");
    }

    @Test
    void rejectsUnsafeLimit() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                10001
        );

        assertThatThrownBy(() -> compiler.compile(dataset, request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void appendsOffsetForPagedRequests() {
        BiQueryRequest request = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                10000,
                20000
        );

        BiCompiledQuery query = compiler.compile(dataset, request, 7L);

        assertThat(query.sql()).endsWith("""
                ORDER BY stat_date ASC
                LIMIT 10000
                OFFSET 20000
                """.stripTrailing());
    }

    @Test
    void bindsSqlDatasetTemplateParametersBeforeTenantAndFilters() {
        BiDatasetSpec sqlDataset = new BiDatasetSpec(
                "campaign_sql",
                "(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset",
                "tenant_id",
                Map.of(
                        "stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE"),
                        "total_cost", new BiFieldSpec("total_cost", "total_cost", BiFieldSpec.Role.MEASURE, "NUMBER")),
                Map.of("total_cost", new BiMetricSpec("total_cost", "SUM(total_cost)", "NUMBER")),
                List.of(
                        new BiSqlParameterSpec("start_date", "DATE", true, null, List.of()),
                        new BiSqlParameterSpec("channel", "STRING", false, "PAID", List.of("PAID", "EMAIL"))));
        BiQueryRequest request = new BiQueryRequest(
                "campaign_sql",
                null,
                List.of("stat_date"),
                List.of("total_cost"),
                List.of(new BiFilter("stat_date", BiFilter.Operator.LTE, "2026-06-30")),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                100,
                0,
                Map.of("start_date", "2026-06-01"));

        BiCompiledQuery query = compiler.compile(sqlDataset, request, 7L);

        assertThat(query.sql()).isEqualTo("""
                SELECT stat_date AS stat_date, SUM(total_cost) AS total_cost
                FROM (SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset
                WHERE tenant_id = ? AND stat_date <= ?
                GROUP BY stat_date
                ORDER BY stat_date ASC
                LIMIT 100
                """.stripTrailing());
        assertThat(query.parameters()).containsExactly("2026-06-01", "PAID", 7L, "2026-06-30");
    }

    @Test
    void rejectsSqlDatasetWhenRequiredTemplateParameterIsMissing() {
        BiDatasetSpec sqlDataset = new BiDatasetSpec(
                "campaign_sql",
                "(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ?) sql_dataset",
                "tenant_id",
                Map.of("stat_date", new BiFieldSpec("stat_date", "stat_date", BiFieldSpec.Role.DIMENSION, "DATE")),
                Map.of("total_cost", new BiMetricSpec("total_cost", "SUM(total_cost)", "NUMBER")),
                List.of(new BiSqlParameterSpec("start_date", "DATE", true, null, List.of())));
        BiQueryRequest request = new BiQueryRequest(
                "campaign_sql",
                null,
                List.of("stat_date"),
                List.of("total_cost"),
                List.of(),
                List.of(),
                100,
                0,
                Map.of());

        assertThatThrownBy(() -> compiler.compile(sqlDataset, request, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL parameter is required: start_date");
    }
}
