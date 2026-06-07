package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.ai.BiAskDataAgentService;
import org.chovy.canvas.domain.bi.ai.BiAskDataPlan;
import org.chovy.canvas.domain.bi.ai.BiAskDataPlanner;
import org.chovy.canvas.domain.bi.ai.BiAskDataPlanningContext;
import org.chovy.canvas.domain.bi.ai.BiAskDataPlanningResult;
import org.chovy.canvas.domain.bi.ai.BiAskDataRequest;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftAgentService;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlan;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanner;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanningContext;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftPlanningResult;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftRequest;
import org.chovy.canvas.domain.bi.ai.BiInsightAgentService;
import org.chovy.canvas.domain.bi.ai.BiInsightPlan;
import org.chovy.canvas.domain.bi.ai.BiInsightPlanner;
import org.chovy.canvas.domain.bi.ai.BiInsightPlanningContext;
import org.chovy.canvas.domain.bi.ai.BiInsightPlanningResult;
import org.chovy.canvas.domain.bi.ai.BiInsightRequest;
import org.chovy.canvas.domain.bi.ai.BiInterpretationAgentService;
import org.chovy.canvas.domain.bi.ai.BiInterpretationPlan;
import org.chovy.canvas.domain.bi.ai.BiInterpretationPlanner;
import org.chovy.canvas.domain.bi.ai.BiInterpretationPlanningContext;
import org.chovy.canvas.domain.bi.ai.BiInterpretationPlanningResult;
import org.chovy.canvas.domain.bi.ai.BiInterpretationRequest;
import org.chovy.canvas.domain.bi.ai.BiReportAgentService;
import org.chovy.canvas.domain.bi.ai.BiReportPlan;
import org.chovy.canvas.domain.bi.ai.BiReportPlanner;
import org.chovy.canvas.domain.bi.ai.BiReportPlanningContext;
import org.chovy.canvas.domain.bi.ai.BiReportPlanningResult;
import org.chovy.canvas.domain.bi.ai.BiReportRequest;
import org.chovy.canvas.domain.bi.ai.BiReportSection;
import org.chovy.canvas.domain.bi.ai.BiReportSectionInput;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardPreset;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardWidget;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryRecorder;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiAiControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneOffset.UTC);

    @Test
    void askDataUsesCurrentTenantContext() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice")));
        RecordingPlanner planner = new RecordingPlanner();
        BiQueryExecutionService queryService = new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                BiQueryHistoryRecorder.noop(),
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                CLOCK);
        BiAiController controller = new BiAiController(
                resolver,
                new BiAskDataAgentService(BiDatasetSpecResolver.builtIn(), planner, queryService));

        StepVerifier.create(controller.askData(new BiAskDataRequest(
                        "show daily executions",
                        "canvas_daily_stats",
                        null,
                        null,
                        null,
                        null,
                        25,
                        Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(planner.context.tenantId()).isEqualTo(7L);
                    assertThat(planner.context.username()).isEqualTo("alice");
                    assertThat(planner.context.role()).isEqualTo(RoleNames.TENANT_ADMIN);
                    assertThat(response.getData().query().limit()).isEqualTo(25);
                    assertThat(response.getData().result().rowCount()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void remainingAiEndpointsUseCurrentTenantContext() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice")));
        RecordingInterpretationPlanner interpretationPlanner = new RecordingInterpretationPlanner();
        RecordingReportPlanner reportPlanner = new RecordingReportPlanner();
        RecordingDashboardDraftPlanner dashboardDraftPlanner = new RecordingDashboardDraftPlanner();
        RecordingInsightPlanner insightPlanner = new RecordingInsightPlanner();
        BiAiController controller = new BiAiController(
                resolver,
                new BiAskDataAgentService(BiDatasetSpecResolver.builtIn(), new RecordingPlanner(), queryService()),
                new BiInterpretationAgentService(BiDatasetSpecResolver.builtIn(), interpretationPlanner),
                new BiReportAgentService(BiDatasetSpecResolver.builtIn(), reportPlanner),
                new BiDashboardDraftAgentService(BiDatasetSpecResolver.builtIn(), dashboardDraftPlanner),
                new BiInsightAgentService(BiDatasetSpecResolver.builtIn(), insightPlanner));

        StepVerifier.create(controller.interpret(new BiInterpretationRequest(
                        "explain",
                        "CHART",
                        "trend-executions",
                        trendQuery(),
                        result(),
                        null,
                        null,
                        null,
                        null,
                        Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(interpretationPlanner.context.tenantId()).isEqualTo(7L);
                    assertThat(interpretationPlanner.context.role()).isEqualTo(RoleNames.TENANT_ADMIN);
                })
                .verifyComplete();

        StepVerifier.create(controller.report(new BiReportRequest(
                        "WEEKLY",
                        "Weekly BI Report",
                        List.of(new BiReportSectionInput("Trend", trendQuery(), result())),
                        null,
                        null,
                        null,
                        null,
                        Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(reportPlanner.context.username()).isEqualTo("alice");
                    assertThat(response.getData().title()).isEqualTo("Weekly BI Report");
                })
                .verifyComplete();

        StepVerifier.create(controller.dashboardDraft(new BiDashboardDraftRequest(
                        "build dashboard",
                        "canvas_daily_stats",
                        null,
                        null,
                        null,
                        null,
                        Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(dashboardDraftPlanner.context.tenantId()).isEqualTo(7L);
                    assertThat(response.getData().dashboard().dashboardKey()).isEqualTo("ai-dashboard");
                })
                .verifyComplete();

        StepVerifier.create(controller.insights(new BiInsightRequest(
                        "find anomalies",
                        trendQuery(),
                        result(),
                        result(),
                        null,
                        null,
                        null,
                        null,
                        Map.of())))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(insightPlanner.context.username()).isEqualTo("alice");
                    assertThat(response.getData().trends()).containsExactly("Trend detected.");
                })
                .verifyComplete();
    }

    private BiQueryExecutionService queryService() {
        return new BiQueryExecutionService(
                new BiQueryCompiler(),
                (query, dataset) -> List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                BiQueryHistoryRecorder.noop(),
                BiQueryResultCache.noop(),
                BiDatasetSpecResolver.builtIn(),
                null,
                null,
                CLOCK);
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

    private BiQueryResult result() {
        return new BiQueryResult(
                "canvas_daily_stats",
                List.of(
                        new BiQueryColumn("stat_date", "DIMENSION", "DATE"),
                        new BiQueryColumn("total_executions", "METRIC", "NUMBER")),
                List.of(Map.of("stat_date", "2026-06-05", "total_executions", 42L)),
                1,
                12,
                "hash");
    }

    private static BiDashboardPreset dashboard() {
        return new BiDashboardPreset(
                "ai-dashboard",
                "AI Dashboard",
                "Generated draft",
                "canvas_daily_stats",
                List.of(new BiDashboardWidget(
                        "trend-executions",
                        "Execution Trend",
                        "LINE",
                        List.of("stat_date"),
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
    }

    private static final class RecordingPlanner implements BiAskDataPlanner {
        private BiAskDataPlanningContext context;

        @Override
        public BiAskDataPlanningResult plan(BiAskDataPlanningContext context) {
            this.context = context;
            return new BiAskDataPlanningResult("SUCCESS", false, new BiAskDataPlan(
                    "canvas_daily_stats",
                    List.of("stat_date"),
                    List.of("total_executions"),
                    List.of(),
                    List.of(),
                    100,
                    "Daily execution trend"));
        }
    }

    private static final class RecordingInterpretationPlanner implements BiInterpretationPlanner {
        private BiInterpretationPlanningContext context;

        @Override
        public BiInterpretationPlanningResult plan(BiInterpretationPlanningContext context) {
            this.context = context;
            return new BiInterpretationPlanningResult("SUCCESS", false, new BiInterpretationPlan(
                    "Summary",
                    List.of("Finding"),
                    List.of("Recommendation")));
        }
    }

    private static final class RecordingReportPlanner implements BiReportPlanner {
        private BiReportPlanningContext context;

        @Override
        public BiReportPlanningResult plan(BiReportPlanningContext context) {
            this.context = context;
            return new BiReportPlanningResult("SUCCESS", false, new BiReportPlan(
                    "Weekly BI Report",
                    "Summary",
                    List.of(new BiReportSection("Trend", "Body")),
                    List.of("Next")));
        }
    }

    private static final class RecordingDashboardDraftPlanner implements BiDashboardDraftPlanner {
        private BiDashboardDraftPlanningContext context;

        @Override
        public BiDashboardDraftPlanningResult plan(BiDashboardDraftPlanningContext context) {
            this.context = context;
            return new BiDashboardDraftPlanningResult("SUCCESS", false, new BiDashboardDraftPlan(
                    dashboard(),
                    List.of(),
                    "Generated"));
        }
    }

    private static final class RecordingInsightPlanner implements BiInsightPlanner {
        private BiInsightPlanningContext context;

        @Override
        public BiInsightPlanningResult plan(BiInsightPlanningContext context) {
            this.context = context;
            return new BiInsightPlanningResult("SUCCESS", false, new BiInsightPlan(
                    List.of("Trend detected."),
                    List.of("Anomaly detected."),
                    List.of("Opportunity detected.")));
        }
    }
}
