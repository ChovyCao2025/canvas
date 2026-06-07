package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.domain.bi.chart.BiChartResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiRemainingAiAgentsTest {

    @Test
    void interpretationAgentUsesSemanticQueryContext() {
        RecordingInterpretationPlanner planner = new RecordingInterpretationPlanner(new BiInterpretationPlan(
                "Executions are trending upward.",
                List.of("Total executions reached 42."),
                List.of("Inspect failing canvases next.")));
        BiInterpretationAgentService service = new BiInterpretationAgentService(
                BiDatasetSpecResolver.builtIn(),
                planner);
        BiQueryRequest query = trendQuery();

        BiInterpretationResponse response = service.interpret(
                new BiInterpretationRequest(
                        "explain this trend",
                        "CHART",
                        "trend-executions",
                        query,
                        result("canvas_daily_stats"),
                        null,
                        null,
                        null,
                        null,
                        Map.of()),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR));

        assertThat(planner.context.tenantId()).isEqualTo(7L);
        assertThat(planner.context.query()).isEqualTo(query);
        assertThat(planner.context.datasets()).singleElement()
                .satisfies(dataset -> assertThat(dataset.metrics()).containsKey("total_executions"));
        assertThat(response.summary()).isEqualTo("Executions are trending upward.");
        assertThat(response.keyFindings()).containsExactly("Total executions reached 42.");
        assertThat(response.recommendations()).containsExactly("Inspect failing canvases next.");
    }

    @Test
    void reportAgentValidatesEverySemanticSectionBeforePlanning() {
        RecordingReportPlanner planner = new RecordingReportPlanner(new BiReportPlan(
                "Weekly BI Report",
                "Execution volume is stable.",
                List.of(new BiReportSection("Trend", "Daily executions remain healthy.")),
                List.of("Review failure spikes.")));
        BiReportAgentService service = new BiReportAgentService(BiDatasetSpecResolver.builtIn(), planner);
        BiReportSectionInput section = new BiReportSectionInput("Trend", trendQuery(), result("canvas_daily_stats"));

        BiReportResponse response = service.generate(
                new BiReportRequest(
                        "WEEKLY",
                        "Weekly BI Report",
                        List.of(section),
                        null,
                        null,
                        null,
                        null,
                        Map.of()),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR));

        assertThat(planner.context.sections()).containsExactly(section);
        assertThat(response.title()).isEqualTo("Weekly BI Report");
        assertThat(response.sections()).singleElement()
                .satisfies(reportSection -> assertThat(reportSection.body()).contains("healthy"));
    }

    @Test
    void dashboardDraftAgentRejectsUnknownGeneratedSemanticFields() {
        BiDashboardPreset unsafePreset = new BiDashboardPreset(
                "ai-dashboard",
                "AI Dashboard",
                "Unsafe draft",
                "canvas_daily_stats",
                List.of(new BiDashboardWidget(
                        "unsafe-widget",
                        "Unsafe",
                        "LINE",
                        List.of("raw_sql"),
                        List.of("total_executions"),
                        0,
                        0,
                        12,
                        6,
                        "time-series")),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        RecordingDashboardDraftPlanner planner = new RecordingDashboardDraftPlanner(new BiDashboardDraftPlan(
                unsafePreset,
                List.of(),
                "Unsafe draft"));
        BiDashboardDraftAgentService service = new BiDashboardDraftAgentService(BiDatasetSpecResolver.builtIn(), planner);

        assertThatThrownBy(() -> service.generate(
                new BiDashboardDraftRequest(
                        "build a dashboard",
                        "canvas_daily_stats",
                        null,
                        null,
                        null,
                        null,
                        Map.of()),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown dimension: raw_sql");
    }

    @Test
    void insightAgentReceivesCurrentAndBaselineSemanticResults() {
        RecordingInsightPlanner planner = new RecordingInsightPlanner(new BiInsightPlan(
                List.of("Execution volume increased."),
                List.of("Failure count is above baseline."),
                List.of("Inspect campaigns with recent edits.")));
        BiInsightAgentService service = new BiInsightAgentService(BiDatasetSpecResolver.builtIn(), planner);
        BiQueryRequest query = trendQuery();

        BiInsightResponse response = service.inspect(
                new BiInsightRequest(
                        "find anomalies",
                        query,
                        result("canvas_daily_stats"),
                        result("canvas_daily_stats"),
                        null,
                        null,
                        null,
                        null,
                        Map.of()),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR));

        assertThat(planner.context.currentResult().datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(planner.context.baselineResult().datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(response.trends()).containsExactly("Execution volume increased.");
        assertThat(response.anomalies()).containsExactly("Failure count is above baseline.");
    }

    private BiQueryRequest trendQuery() {
        return new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(),
                100);
    }

    private BiQueryResult result(String datasetKey) {
        return new BiQueryResult(
                datasetKey,
                List.of(
                        new BiQueryColumn("stat_date", "DIMENSION", "DATE"),
                        new BiQueryColumn("total_executions", "METRIC", "NUMBER")),
                List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                1,
                12,
                "hash");
    }

    private static final class RecordingInterpretationPlanner implements BiInterpretationPlanner {
        private final BiInterpretationPlan plan;
        private BiInterpretationPlanningContext context;

        private RecordingInterpretationPlanner(BiInterpretationPlan plan) {
            this.plan = plan;
        }

        @Override
        public BiInterpretationPlanningResult plan(BiInterpretationPlanningContext context) {
            this.context = context;
            return new BiInterpretationPlanningResult("SUCCESS", false, plan);
        }
    }

    private static final class RecordingReportPlanner implements BiReportPlanner {
        private final BiReportPlan plan;
        private BiReportPlanningContext context;

        private RecordingReportPlanner(BiReportPlan plan) {
            this.plan = plan;
        }

        @Override
        public BiReportPlanningResult plan(BiReportPlanningContext context) {
            this.context = context;
            return new BiReportPlanningResult("SUCCESS", false, plan);
        }
    }

    private static final class RecordingDashboardDraftPlanner implements BiDashboardDraftPlanner {
        private final BiDashboardDraftPlan plan;

        private RecordingDashboardDraftPlanner(BiDashboardDraftPlan plan) {
            this.plan = plan;
        }

        @Override
        public BiDashboardDraftPlanningResult plan(BiDashboardDraftPlanningContext context) {
            return new BiDashboardDraftPlanningResult("SUCCESS", false, plan);
        }
    }

    private static final class RecordingInsightPlanner implements BiInsightPlanner {
        private final BiInsightPlan plan;
        private BiInsightPlanningContext context;

        private RecordingInsightPlanner(BiInsightPlan plan) {
            this.plan = plan;
        }

        @Override
        public BiInsightPlanningResult plan(BiInsightPlanningContext context) {
            this.context = context;
            return new BiInsightPlanningResult("SUCCESS", false, plan);
        }
    }
}
