package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiAskDataAgentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneOffset.UTC);

    @Test
    void plansFromSemanticCatalogAndExecutesStructuredQueryThroughCurrentTenant() {
        RecordingPlanner planner = new RecordingPlanner(new BiAskDataPlan(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                500,
                "Daily execution trend"));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        BiQueryExecutionService queryService = queryService((query, dataset) -> {
            compiledQueries.add(query);
            return List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L));
        });
        BiAskDataAgentService service = new BiAskDataAgentService(
                BiDatasetSpecResolver.builtIn(),
                planner,
                queryService);

        BiAskDataResponse response = service.ask(
                new BiAskDataRequest(
                        "show daily executions",
                        "canvas_daily_stats",
                        null,
                        null,
                        null,
                        null,
                        100,
                        Map.of("temperature", 0.1)),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR));

        assertThat(planner.context).isNotNull();
        assertThat(planner.context.tenantId()).isEqualTo(7L);
        assertThat(planner.context.question()).isEqualTo("show daily executions");
        assertThat(planner.context.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.datasetKey()).isEqualTo("canvas_daily_stats");
            assertThat(dataset.fields()).containsKeys("stat_date", "canvas_name");
            assertThat(dataset.metrics()).containsKeys("total_executions", "success_rate");
        });
        assertThat(response.query()).isEqualTo(new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100));
        assertThat(response.explanation()).isEqualTo("Daily execution trend");
        assertThat(response.result().rowCount()).isEqualTo(1);
        assertThat(compiledQueries).singleElement().satisfies(query -> {
            assertThat(query.sql()).contains("tenant_id = ?");
            assertThat(query.parameters()).containsExactly(7L);
        });
    }

    @Test
    void rejectsUnknownPlannerFieldsBeforeDatasourceExecution() {
        RecordingPlanner planner = new RecordingPlanner(new BiAskDataPlan(
                "canvas_daily_stats",
                List.of("raw_sql"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100,
                "Unsafe direct field"));
        List<BiCompiledQuery> compiledQueries = new ArrayList<>();
        BiAskDataAgentService service = new BiAskDataAgentService(
                BiDatasetSpecResolver.builtIn(),
                planner,
                queryService((query, dataset) -> {
                    compiledQueries.add(query);
                    return List.of(Map.of());
                }));

        assertThatThrownBy(() -> service.ask(
                new BiAskDataRequest("show raw sql", null, null, null, null, null, 100, Map.of()),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown dimension: raw_sql");
        assertThat(compiledQueries).isEmpty();
    }

    private BiQueryExecutionService queryService(BiDatasource datasource) {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                datasource::execute,
                BiQueryHistoryRecorder.noop(),
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                CLOCK);
    }

    @FunctionalInterface
    private interface BiDatasource {
        List<Map<String, Object>> execute(BiCompiledQuery query, BiDatasetSpec dataset);
    }

    private static final class RecordingPlanner implements BiAskDataPlanner {
        private final BiAskDataPlan plan;
        private BiAskDataPlanningContext context;

        private RecordingPlanner(BiAskDataPlan plan) {
            this.plan = plan;
        }

        @Override
        public BiAskDataPlanningResult plan(BiAskDataPlanningContext context) {
            this.context = context;
            return new BiAskDataPlanningResult("SUCCESS", false, plan);
        }
    }
}
