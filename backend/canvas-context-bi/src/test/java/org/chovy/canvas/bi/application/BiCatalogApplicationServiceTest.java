package org.chovy.canvas.bi.application;

import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiWorkspaceCommand;
import org.chovy.canvas.bi.adapter.persistence.BiChartDO;
import org.chovy.canvas.bi.adapter.persistence.BiChartMapper;
import org.chovy.canvas.bi.adapter.persistence.BiDashboardDO;
import org.chovy.canvas.bi.adapter.persistence.BiDashboardMapper;
import org.chovy.canvas.bi.adapter.persistence.BiDashboardWidgetMapper;
import org.chovy.canvas.bi.adapter.persistence.BiDatasetDO;
import org.chovy.canvas.bi.adapter.persistence.BiDatasetFieldMapper;
import org.chovy.canvas.bi.adapter.persistence.BiDatasetMapper;
import org.chovy.canvas.bi.adapter.persistence.BiMetricMapper;
import org.chovy.canvas.bi.adapter.persistence.BiPersistenceConverter;
import org.chovy.canvas.bi.adapter.persistence.BiWorkspaceDO;
import org.chovy.canvas.bi.adapter.persistence.BiResourcePermissionMapper;
import org.chovy.canvas.bi.adapter.persistence.BiWorkspaceMapper;
import org.chovy.canvas.bi.adapter.persistence.MybatisBiCatalogRepository;
import org.chovy.canvas.bi.domain.BiAccessRequest;
import org.chovy.canvas.bi.domain.BiChart;
import org.chovy.canvas.bi.domain.BiChartRepository;
import org.chovy.canvas.bi.domain.BiDashboard;
import org.chovy.canvas.bi.domain.BiDashboardRepository;
import org.chovy.canvas.bi.domain.BiDataset;
import org.chovy.canvas.bi.domain.BiDatasetRepository;
import org.chovy.canvas.bi.domain.BiPermissionGrant;
import org.chovy.canvas.bi.domain.BiPermissionRepository;
import org.chovy.canvas.bi.domain.BiResourceKey;
import org.chovy.canvas.bi.domain.BiResourceStatus;
import org.chovy.canvas.bi.domain.BiWorkspace;
import org.chovy.canvas.bi.domain.BiWorkspaceRepository;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiCatalogApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void upsertDatasetNormalizesBusinessKeyAndRetainsFieldMetricContracts() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveWorkspace(workspace(5L));

        BiDatasetView view = service.upsertDataset(7L, new BiDatasetCommand(
                5L,
                " Orders Daily ",
                "Orders daily",
                "sql",
                99L,
                "fact_order",
                "tenant_id",
                Map.of("grain", "day"),
                List.of(new BiDatasetFieldCommand(
                        "order_date",
                        "Order date",
                        "order_date",
                        "DIMENSION",
                        "DATE",
                        "NONE",
                        true,
                        1)),
                List.of(new BiMetricCommand(
                        "gmv",
                        "GMV",
                        "sum(pay_amount)",
                        "SUM",
                        "DECIMAL",
                        "CNY")),
                "draft"), "analyst");

        assertThat(view.datasetKey()).isEqualTo("orders-daily");
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.fields()).singleElement()
                .satisfies(field -> assertThat(field.fieldKey()).isEqualTo("order-date"));
        assertThat(view.metrics()).singleElement()
                .satisfies(metric -> assertThat(metric.metricKey()).isEqualTo("gmv"));
        assertThat(repository.datasetsById.get(100L).model()).containsEntry("grain", "day");
    }

    @Test
    void chartCannotUseMissingOrArchivedDataset() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveWorkspace(workspace(5L));
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.ARCHIVED));

        assertThatThrownBy(() -> service.upsertChart(7L, new BiChartCommand(
                5L,
                "orders-trend",
                "Orders trend",
                "line",
                "orders-daily",
                Map.of("dimensions", List.of("date")),
                Map.of(),
                Map.of(),
                "draft"), "analyst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataset is not available for BI chart");
    }

    @Test
    void listDatasetResourcesExcludesArchivedAndPreservesRepositoryOrdering() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDataset(dataset(100L, "beta", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveDataset(dataset(101L, "alpha", BiResourceStatus.DRAFT,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveDataset(dataset(102L, "archived", BiResourceStatus.ARCHIVED,
                LocalDateTime.parse("2026-06-04T00:00:00")));
        repository.saveDataset(dataset(103L, "latest", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-05T00:00:00")));

        List<BiDatasetView> views = service.listDatasetResources(7L);

        assertThat(views).extracting(BiDatasetView::datasetKey)
                .containsExactly("latest", "alpha", "beta");
        assertThat(views).extracting(BiDatasetView::status)
                .doesNotContain(BiResourceStatus.ARCHIVED.name());
    }

    @Test
    void getDatasetResourceResolvesByDatasetKeyAndRejectsMissingOrArchived() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));
        repository.saveDataset(dataset(101L, "archived-dataset", BiResourceStatus.ARCHIVED));

        BiDatasetView view = service.getDatasetResource(7L, "orders-daily");

        assertThat(view.datasetKey()).isEqualTo("orders-daily");
        assertThatThrownBy(() -> service.getDatasetResource(7L, "archived-dataset"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI dataset not found");
        assertThatThrownBy(() -> service.getDatasetResource(7L, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI dataset not found");
    }

    @Test
    void queryDatasetCatalogReturnsCompactSortedBuiltInViewsWithoutInternalSqlDetails() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        List<BiQueryDatasetView> views = service.listQueryDatasets(7L);
        BiQueryDatasetView detail = service.getQueryDataset(42L, "canvas_daily_stats");

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.datasetKey()).isEqualTo("canvas_daily_stats"));
        assertThat(detail.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(detail.fields()).extracting("fieldKey").containsExactly(
                "avg_duration_ms",
                "canvas_id",
                "canvas_name",
                "fail_count",
                "running_count",
                "stat_date",
                "success_count",
                "total_executions",
                "trigger_type",
                "unique_users");
        assertThat(detail.fields())
                .anySatisfy(field -> {
                    assertThat(field.fieldKey()).isEqualTo("stat_date");
                    assertThat(field.role()).isEqualTo("DIMENSION");
                    assertThat(field.dataType()).isEqualTo("DATE");
                })
                .anySatisfy(field -> {
                    assertThat(field.fieldKey()).isEqualTo("canvas_name");
                    assertThat(field.role()).isEqualTo("DIMENSION");
                    assertThat(field.dataType()).isEqualTo("STRING");
                })
                .anySatisfy(field -> {
                    assertThat(field.fieldKey()).isEqualTo("trigger_type");
                    assertThat(field.role()).isEqualTo("DIMENSION");
                    assertThat(field.dataType()).isEqualTo("STRING");
                });
        assertThat(detail.metrics()).extracting("metricKey").containsExactly(
                "avg_duration_ms",
                "fail_count",
                "success_count",
                "success_rate",
                "total_executions",
                "unique_users");
        assertThat(detail.metrics())
                .anySatisfy(metric -> {
                    assertThat(metric.metricKey()).isEqualTo("total_executions");
                    assertThat(metric.dataType()).isEqualTo("NUMBER");
                })
                .anySatisfy(metric -> {
                    assertThat(metric.metricKey()).isEqualTo("success_rate");
                    assertThat(metric.dataType()).isEqualTo("PERCENT");
                });
        assertThat(recordComponentNames(BiQueryDatasetView.class))
                .containsExactly("datasetKey", "fields", "metrics");

        assertThatThrownBy(() -> service.getQueryDataset(7L, "missing_dataset"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown BI dataset: missing_dataset");
    }

    @Test
    void dashboardPresetCatalogReturnsCompactBuiltInViewsAndRejectsUnknownKey() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        List<BiDashboardPresetView> views = service.listDashboardPresets(7L);
        BiDashboardPresetView detail = service.getDashboardPreset(42L, "canvas-effect");

        assertThat(views).singleElement()
                .satisfies(view -> assertThat(view.dashboardKey()).isEqualTo("canvas-effect"));
        assertThat(detail.dashboardKey()).isEqualTo("canvas-effect");
        assertThat(detail.title()).isEqualTo("画布效果分析");
        assertThat(detail.description()).contains("QuickBI-like");
        assertThat(detail.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(detail.widgets()).extracting("widgetKey").containsExactly(
                "kpi-total-executions",
                "kpi-success-rate",
                "trend-executions",
                "rank-canvas",
                "detail-canvas");
        assertThat(detail.widgets())
                .anySatisfy(widget -> {
                    assertThat(widget.widgetKey()).isEqualTo("trend-executions");
                    assertThat(widget.chartType()).isEqualTo("LINE");
                    assertThat(widget.dimensions()).containsExactly("stat_date");
                    assertThat(widget.metrics()).containsExactly("total_executions", "success_count", "fail_count");
                    assertThat(widget.gridW()).isEqualTo(12);
                    assertThat(widget.gridH()).isEqualTo(6);
                    assertThat(widget.stylePreset()).isEqualTo("time-series");
                });
        assertThat(detail.filters()).extracting("filterKey").containsExactly(
                "filter-stat-date",
                "filter-canvas",
                "filter-trigger-type");
        assertThat(detail.filters())
                .anySatisfy(filter -> {
                    assertThat(filter.filterKey()).isEqualTo("filter-canvas");
                    assertThat(filter.fieldKey()).isEqualTo("canvas_name");
                    assertThat(filter.controlType()).isEqualTo("SEARCH_SELECT");
                    assertThat(filter.parentFilterKeys()).containsExactly("filter-stat-date");
                    assertThat(filter.cascadeMode()).isEqualTo("SAME_SOURCE");
                });
        assertThat(detail.interactions()).extracting("interactionKey").containsExactly(
                "linkage-trend-to-detail",
                "drill-rank-canvas",
                "open-canvas-stats");
        assertThat(detail.interactions())
                .anySatisfy(interaction -> {
                    assertThat(interaction.interactionKey()).isEqualTo("open-canvas-stats");
                    assertThat(interaction.interactionType()).isEqualTo("HYPERLINK");
                    assertThat(interaction.target()).isEqualTo("/canvas/{canvas_id}/stats");
                });
        assertThat(detail.subscriptionChannels()).containsExactly("EMAIL", "LARK", "WEBHOOK");
        assertThat(detail.embedScopes()).containsExactly("INTERNAL_CANVAS", "EXTERNAL_TICKET");
        assertThat(recordComponentNames(BiDashboardPresetView.class)).containsExactly(
                "dashboardKey",
                "title",
                "description",
                "datasetKey",
                "widgets",
                "filters",
                "interactions",
                "subscriptionChannels",
                "embedScopes");

        assertThatThrownBy(() -> service.getDashboardPreset(7L, "missing-dashboard"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown BI dashboard preset: missing-dashboard");
    }

    @Test
    void quickEngineCapacityCatalogReturnsCompactSummaryAndClampsLimit() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiQuickEngineCapacitySummaryView summary = service.quickEngineCapacity(7L, -10);

        assertThat(summary.tenantId()).isEqualTo(7L);
        assertThat(summary.capacityLimitRows()).isEqualTo(1_000_000L);
        assertThat(summary.usedRows()).isEqualTo(420_000L);
        assertThat(summary.usagePercent()).isEqualTo(42.0);
        assertThat(summary.alertLevel()).isEqualTo("NORMAL");
        assertThat(summary.alertEnabled()).isFalse();
        assertThat(summary.alertPolicy()).containsEntry("warningThresholdPercent", 80);
        assertThat(summary.alertPolicy()).containsEntry("enabled", false);
        assertThat(summary.alertPolicy()).containsEntry("notificationChannels", List.of());
        assertThat(summary.alertPolicy()).containsEntry("notificationReceivers", List.of());
        assertThat(summary.alertPolicy()).doesNotContainKey("receivers");
        assertThat(summary.tenantPoolPolicy().poolKey()).isEqualTo("STANDARD");
        assertThat(summary.concurrencyQueue()).containsEntry("runningQueries", 2);
        assertThat(summary.details()).hasSize(1);
        assertThat(summary.details()).singleElement()
                .satisfies(detail -> {
                    assertThat(detail).containsEntry("resourceKey", "canvas_daily_stats");
                    assertThat(detail).containsEntry("usedRows", 240_000L);
                });
        assertThat(summary.userRankings()).singleElement()
                .satisfies(user -> assertThat(user).containsEntry("username", "analyst"));
        assertThat(recordComponentNames(BiQuickEngineCapacitySummaryView.class)).containsExactly(
                "tenantId",
                "capacityLimitRows",
                "usedRows",
                "usagePercent",
                "alertLevel",
                "alertEnabled",
                "alertPolicy",
                "tenantPoolPolicy",
                "concurrencyQueue",
                "categories",
                "details",
                "userRankings");
    }

    @Test
    void quickEngineQueueCatalogNormalizesFiltersAndLimit() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiQuickEngineQueueSnapshotView snapshot = service.quickEngineQueue(7L, " gold ", " queued ", 1_000);

        assertThat(snapshot.tenantId()).isEqualTo(7L);
        assertThat(snapshot.poolKey()).isEqualTo("GOLD");
        assertThat(snapshot.queued()).isEqualTo(3L);
        assertThat(snapshot.claimed()).isEqualTo(2L);
        assertThat(snapshot.completed()).isEqualTo(9L);
        assertThat(snapshot.blocked()).isEqualTo(1L);
        assertThat(snapshot.total()).isEqualTo(15L);
        assertThat(snapshot.jobs()).hasSize(2);
        assertThat(snapshot.jobs()).extracting("tenantId").containsOnly(7L);
        assertThat(snapshot.jobs()).extracting("poolKey").containsOnly("GOLD");
        assertThat(snapshot.jobs()).extracting("status").containsOnly("QUEUED");
        assertThat(snapshot.jobs()).extracting("attemptCount").containsExactly(1, 0);
        assertThat(recordComponentNames(snapshot.jobs().getFirst().getClass())).containsExactly(
                "id",
                "tenantId",
                "poolKey",
                "sqlHash",
                "datasetKey",
                "requestedBy",
                "status",
                "attemptCount",
                "queuedAt",
                "expiresAt",
                "claimedBy",
                "claimedAt",
                "completedAt",
                "blockedReason",
                "createdAt",
                "updatedAt");
        assertThat(recordComponentNames(BiQuickEngineQueueSnapshotView.class)).containsExactly(
                "tenantId",
                "poolKey",
                "queued",
                "claimed",
                "completed",
                "blocked",
                "total",
                "jobs");
    }

    @Test
    void quickEngineCapacityPoliciesNormalizeInputPreserveDefaultsAndValidateLegacyBounds() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiQuickEngineCapacityAlertPolicyView alertPolicy = service.updateQuickEngineCapacityAlertPolicy(
                7L,
                new BiQuickEngineCapacityAlertPolicyCommand(
                        null,
                        2_000_000L,
                        70,
                        90,
                        List.of(" email ", "LARK", "EMAIL", " "),
                        List.of(" bi-ops ", " analyst ", "bi-ops")),
                " operator ");
        BiQuickEngineTenantPoolPolicyView poolPolicy = service.updateQuickEngineTenantPoolPolicy(
                7L,
                new BiQuickEngineTenantPoolPolicyCommand(" gold_pool ", 16, null, 300, 250),
                " operator ");

        assertThat(alertPolicy.enabled()).isFalse();
        assertThat(alertPolicy.capacityLimitRows()).isEqualTo(2_000_000L);
        assertThat(alertPolicy.warningThresholdPercent()).isEqualTo(70);
        assertThat(alertPolicy.criticalThresholdPercent()).isEqualTo(90);
        assertThat(alertPolicy.notificationChannels()).containsExactly("EMAIL", "LARK");
        assertThat(alertPolicy.notificationReceivers()).containsExactly("bi-ops", "analyst");
        assertThat(alertPolicy.updatedBy()).isEqualTo("operator");
        assertThat(alertPolicy.updatedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(recordComponentNames(BiQuickEngineCapacityAlertPolicyView.class)).containsExactly(
                "enabled",
                "capacityLimitRows",
                "warningThresholdPercent",
                "criticalThresholdPercent",
                "notificationChannels",
                "notificationReceivers",
                "updatedBy",
                "updatedAt");

        assertThat(poolPolicy.poolKey()).isEqualTo("GOLD_POOL");
        assertThat(poolPolicy.maxConcurrentQueries()).isEqualTo(16);
        assertThat(poolPolicy.queueLimit()).isEqualTo(50);
        assertThat(poolPolicy.queueTimeoutSeconds()).isEqualTo(300);
        assertThat(poolPolicy.poolWeight()).isEqualTo(250);
        assertThat(poolPolicy.updatedBy()).isEqualTo("operator");
        assertThat(poolPolicy.updatedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:00:00"));
        assertThat(recordComponentNames(BiQuickEngineTenantPoolPolicyView.class)).containsExactly(
                "poolKey",
                "maxConcurrentQueries",
                "queueLimit",
                "queueTimeoutSeconds",
                "poolWeight",
                "updatedBy",
                "updatedAt");

        BiQuickEngineCapacityAlertPolicyView preservedDefaultAlertPolicy =
                service.updateQuickEngineCapacityAlertPolicy(8L, null, "system");
        BiQuickEngineTenantPoolPolicyView preservedDefaultPoolPolicy =
                service.updateQuickEngineTenantPoolPolicy(8L, null, "system");
        assertThat(preservedDefaultAlertPolicy.enabled()).isFalse();
        assertThat(preservedDefaultAlertPolicy.notificationChannels()).isEmpty();
        assertThat(preservedDefaultAlertPolicy.notificationReceivers()).isEmpty();
        assertThat(preservedDefaultPoolPolicy.poolKey()).isEqualTo("STANDARD");

        assertThatThrownBy(() -> service.updateQuickEngineCapacityAlertPolicy(7L,
                new BiQuickEngineCapacityAlertPolicyCommand(true, 0L, 80, 95, null, null), "analyst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacityLimitRows must be positive");
        assertThatThrownBy(() -> service.updateQuickEngineCapacityAlertPolicy(7L,
                new BiQuickEngineCapacityAlertPolicyCommand(true, 1_000_000L, 95, 95, null, null), "analyst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("warningThresholdPercent must be less than criticalThresholdPercent");
        assertThatThrownBy(() -> service.updateQuickEngineTenantPoolPolicy(7L,
                new BiQuickEngineTenantPoolPolicyCommand("bad key", 1, 1, 1, 1), "analyst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("poolKey must match");
        assertThatThrownBy(() -> service.updateQuickEngineTenantPoolPolicy(7L,
                new BiQuickEngineTenantPoolPolicyCommand("GOLD", 10_001, 1, 1, 1), "analyst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxConcurrentQueries must be between 1 and 10000");
    }

    @Test
    void listChartResourcesExcludesArchivedAndPreservesRepositoryOrdering() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));
        repository.saveChart(chart(200L, "beta", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveChart(chart(201L, "alpha", BiResourceStatus.DRAFT,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveChart(chart(202L, "archived", BiResourceStatus.ARCHIVED,
                LocalDateTime.parse("2026-06-04T00:00:00")));
        repository.saveChart(chart(203L, "latest", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-05T00:00:00")));

        List<BiChartView> views = service.listChartResources(7L);

        assertThat(views).extracting(BiChartView::chartKey)
                .containsExactly("latest", "alpha", "beta");
        assertThat(views).extracting(BiChartView::status)
                .doesNotContain(BiResourceStatus.ARCHIVED.name());
    }

    @Test
    void getChartResourceResolvesByChartKeyAndRejectsMissingOrArchived() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));
        repository.saveChart(chart(200L, "orders-trend", BiResourceStatus.PUBLISHED));
        repository.saveChart(chart(201L, "archived-chart", BiResourceStatus.ARCHIVED));

        BiChartView view = service.getChartResource(7L, "orders-trend");

        assertThat(view.chartKey()).isEqualTo("orders-trend");
        assertThat(view.datasetKey()).isEqualTo("orders-daily");
        assertThatThrownBy(() -> service.getChartResource(7L, "archived-chart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI chart not found");
        assertThatThrownBy(() -> service.getChartResource(7L, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI chart not found");
    }

    @Test
    void chartReferenceImpactReturnsCompactDashboardReferencesAndRejectsMissingOrArchivedChart() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));
        repository.saveChart(chart(200L, "orders-trend", BiResourceStatus.PUBLISHED));
        repository.saveChart(chart(201L, "archived-chart", BiResourceStatus.ARCHIVED));
        repository.saveDashboard(dashboard(300L, "z-overview", BiResourceStatus.PUBLISHED));
        repository.saveDashboard(new BiDashboard(
                301L,
                7L,
                5L,
                BiResourceKey.of("a-overview", "dashboardKey"),
                "A overview",
                "Executive daily view",
                Map.of("mode", "light"),
                Map.of("region", "CN"),
                List.of("orders-trend"),
                BiResourceStatus.DRAFT,
                1,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")));
        repository.saveDashboard(new BiDashboard(
                302L,
                7L,
                5L,
                BiResourceKey.of("archived-overview", "dashboardKey"),
                "Archived overview",
                "Executive daily view",
                Map.of("mode", "light"),
                Map.of("region", "CN"),
                List.of("orders-trend"),
                BiResourceStatus.ARCHIVED,
                1,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")));

        BiChartReferenceImpactView view = service.chartReferenceImpact(7L, "orders-trend");

        assertThat(view.chartKey()).isEqualTo("orders-trend");
        assertThat(view.chartName()).isEqualTo("Orders trend");
        assertThat(view.datasetKey()).isEqualTo("orders-daily");
        assertThat(view.dashboards()).extracting("dashboardKey").containsExactly("a-overview", "z-overview");
        assertThat(view.dashboards())
                .allSatisfy(reference -> {
                    assertThat(reference.widgetKey()).isEqualTo("orders-trend");
                    assertThat(reference.widgetTitle()).isEqualTo("Orders trend");
                });
        assertThat(view.portals()).isEmpty();
        assertThat(view.subscriptions()).isEmpty();
        assertThat(recordComponentNames(BiChartReferenceImpactView.class)).containsExactly(
                "chartKey",
                "chartName",
                "datasetKey",
                "dashboards",
                "portals",
                "subscriptions");

        assertThatThrownBy(() -> service.chartReferenceImpact(7L, "archived-chart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI chart not found: archived-chart");
        assertThatThrownBy(() -> service.chartReferenceImpact(7L, "missing-chart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI chart not found: missing-chart");
    }

    @Test
    void listDashboardResourcesExcludesArchivedAndPreservesRepositoryOrdering() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDashboard(dashboard(300L, "beta", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveDashboard(dashboard(301L, "alpha", BiResourceStatus.DRAFT,
                LocalDateTime.parse("2026-06-03T00:00:00")));
        repository.saveDashboard(dashboard(302L, "archived", BiResourceStatus.ARCHIVED,
                LocalDateTime.parse("2026-06-04T00:00:00")));
        repository.saveDashboard(dashboard(303L, "latest", BiResourceStatus.PUBLISHED,
                LocalDateTime.parse("2026-06-05T00:00:00")));

        List<BiDashboardView> views = service.listDashboardResources(7L);

        assertThat(views).extracting(BiDashboardView::dashboardKey)
                .containsExactly("latest", "alpha", "beta");
        assertThat(views).extracting(BiDashboardView::status)
                .doesNotContain(BiResourceStatus.ARCHIVED.name());
    }

    @Test
    void getDashboardResourceResolvesByDashboardKeyAndRejectsMissingOrArchived() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveDashboard(dashboard(300L, "marketing-overview", BiResourceStatus.PUBLISHED));
        repository.saveDashboard(dashboard(301L, "archived-dashboard", BiResourceStatus.ARCHIVED));

        BiDashboardView view = service.getDashboardResource(7L, "marketing-overview");

        assertThat(view.dashboardKey()).isEqualTo("marketing-overview");
        assertThat(view.chartKeys()).containsExactly("orders-trend");
        assertThatThrownBy(() -> service.getDashboardResource(7L, "archived-dashboard"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI dashboard not found");
        assertThatThrownBy(() -> service.getDashboardResource(7L, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI dashboard not found");
    }

    @Test
    void repositoryListDatasetResourcesFiltersTenantExcludesArchivedAndOrdersCatalogRows() {
        initBiDatasetTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithMappers(workspaceMapper, datasetMapper);
        when(workspaceMapper.selectOne(any())).thenReturn(workspaceRow(5L, 7L));
        when(datasetMapper.selectList(any())).thenReturn(List.of());

        repository.listAvailableDatasets(7L);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(datasetMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =")
                .contains("status <>")
                .contains("ORDER BY")
                .contains("updated_at DESC")
                .contains("dataset_key ASC");
    }

    @Test
    void repositoryDatasetDetailFallsBackFromTenantToTenantZero() {
        initBiDatasetTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithMappers(workspaceMapper, datasetMapper);
        when(workspaceMapper.selectOne(any()))
                .thenReturn(workspaceRow(5L, 7L))
                .thenReturn(workspaceRow(50L, 0L));
        when(datasetMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(datasetRow(900L, 0L, 50L, "orders-daily", BiResourceStatus.PUBLISHED));

        BiDataset dataset = repository.findAvailableDatasetByKeyWithTenantFallback(
                7L,
                BiResourceKey.of("orders-daily", "datasetKey"));

        assertThat(dataset).isNotNull();
        assertThat(dataset.tenantId()).isEqualTo(0L);
        assertThat(dataset.datasetKey().value()).isEqualTo("orders-daily");
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(datasetMapper, org.mockito.Mockito.times(2)).selectOne(captor.capture());
        assertThat(captor.getAllValues().get(0).getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =");
        assertThat(captor.getAllValues().get(1).getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =");
    }

    @Test
    void repositoryListChartResourcesFiltersTenantDefaultWorkspaceExcludesArchivedAndOrdersCatalogRows() {
        initBiChartTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithChartMapper(workspaceMapper, chartMapper);
        when(workspaceMapper.selectOne(any())).thenReturn(workspaceRow(5L, 7L));
        when(chartMapper.selectList(any())).thenReturn(List.of());

        repository.listAvailableCharts(7L);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(chartMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =")
                .contains("status <>")
                .contains("ORDER BY")
                .contains("updated_at DESC")
                .contains("chart_key ASC");
    }

    @Test
    void repositoryChartDetailUsesTenantDefaultWorkspaceWithoutTenantZeroFallback() {
        initBiChartTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithChartMapper(workspaceMapper, chartMapper);
        when(workspaceMapper.selectOne(any())).thenReturn(workspaceRow(5L, 7L));
        when(chartMapper.selectOne(any())).thenReturn(null);

        BiChart chart = repository.findAvailableChartByKey(7L, BiResourceKey.of("orders-trend", "chartKey"));

        assertThat(chart).isNull();
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(chartMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =")
                .contains("chart_key =")
                .contains("status <>")
                .contains("LIMIT 1");
    }

    @Test
    void repositoryListDashboardResourcesFiltersTenantDefaultWorkspaceExcludesArchivedAndOrdersCatalogRows() {
        initBiDashboardTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithDashboardMapper(workspaceMapper, dashboardMapper);
        when(workspaceMapper.selectOne(any())).thenReturn(workspaceRow(5L, 7L));
        when(dashboardMapper.selectList(any())).thenReturn(List.of());

        repository.listAvailableDashboards(7L);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(dashboardMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =")
                .contains("status <>")
                .contains("ORDER BY")
                .contains("updated_at DESC")
                .contains("dashboard_key ASC");
    }

    @Test
    void repositoryDashboardDetailUsesTenantDefaultWorkspaceWithoutTenantZeroFallback() {
        initBiDashboardTableInfo();
        initBiWorkspaceTableInfo();
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        MybatisBiCatalogRepository repository = repositoryWithDashboardMapper(workspaceMapper, dashboardMapper);
        when(workspaceMapper.selectOne(any())).thenReturn(workspaceRow(5L, 7L));
        when(dashboardMapper.selectOne(any())).thenReturn(null);

        BiDashboard dashboard = repository.findAvailableDashboardByKey(
                7L,
                BiResourceKey.of("marketing-overview", "dashboardKey"));

        assertThat(dashboard).isNull();
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(dashboardMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("tenant_id =")
                .contains("workspace_id =")
                .contains("dashboard_key =")
                .contains("status <>")
                .contains("LIMIT 1");
    }

    @Test
    void dashboardReadModelIncludesChartsAndReadinessForCanvasWeb() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveWorkspace(workspace(5L));
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));

        BiChartView chart = service.upsertChart(7L, new BiChartCommand(
                5L,
                "orders-trend",
                "Orders trend",
                "line",
                "orders-daily",
                Map.of("dimensions", List.of("date")),
                Map.of("palette", "ops"),
                Map.of(),
                "published"), "analyst");
        service.upsertDashboard(7L, new BiDashboardCommand(
                5L,
                "marketing-overview",
                "Marketing overview",
                "Executive daily view",
                Map.of("theme", "light"),
                Map.of("region", "CN"),
                List.of(chart.chartKey(), "missing-chart"),
                "draft"), "analyst");

        BiDashboardReadModelView readModel = service.dashboardReadModel(7L, 5L, "marketing-overview");

        assertThat(readModel.dashboard().dashboardKey()).isEqualTo("marketing-overview");
        assertThat(readModel.charts()).extracting(BiChartView::chartKey).containsExactly("orders-trend");
        assertThat(readModel.readiness().productionReady()).isFalse();
        assertThat(readModel.readiness().blockers())
                .anySatisfy(issue -> assertThat(issue.code()).isEqualTo("MISSING_CHART"));
    }

    @Test
    void permissionDecisionIsExposedThroughStableApiView() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        service.grantPermission(7L, new BiPermissionGrantCommand(
                5L,
                "dashboard",
                10L,
                "ALL",
                "*",
                "view",
                "allow"), "admin");
        service.grantPermission(7L, new BiPermissionGrantCommand(
                5L,
                "dashboard",
                10L,
                "USER",
                "alice",
                "view",
                "deny"), "admin");

        BiPermissionDecisionView view = service.effectiveAccess(new BiAccessRequest(
                7L,
                5L,
                "dashboard",
                10L,
                "alice",
                Set.of("analyst"),
                "view"));

        assertThat(view.allowed()).isFalse();
        assertThat(view.effect()).isEqualTo("DENY");
        assertThat(view.signature()).contains("DASHBOARD:10:VIEW:DENY:USER:alice");
    }

    @Test
    void resourceFavoritesAreTenantActorScopedNormalizedDeduplicatedFilteredOrderedAndDeletedIdempotently() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiResourceFavoriteView first = service.favoriteResource(7L, new BiResourceFavoriteCommand(
                " Dashboard ",
                " Marketing Overview ",
                "Marketing overview"), " analyst ");
        BiResourceFavoriteView repeated = service.favoriteResource(7L, new BiResourceFavoriteCommand(
                "dashboard",
                "marketing-overview",
                "Renamed title should not duplicate"), "analyst");
        service.favoriteResource(7L, new BiResourceFavoriteCommand(
                "chart",
                "Orders Trend",
                "Orders trend"), "analyst");
        service.favoriteResource(7L, new BiResourceFavoriteCommand(
                "dashboard",
                "Executive Overview",
                "Executive overview"), "operator");
        service.favoriteResource(42L, new BiResourceFavoriteCommand(
                "dashboard",
                "Tenant 42 Overview",
                "Tenant 42 overview"), "analyst");

        assertThat(first.resourceType()).isEqualTo("DASHBOARD");
        assertThat(first.resourceKey()).isEqualTo("marketing-overview");
        assertThat(first.actor()).isEqualTo("analyst");
        assertThat(repeated).isEqualTo(first);

        assertThat(service.listFavoriteResources(7L, "analyst", null))
                .extracting(BiResourceFavoriteView::resourceType, BiResourceFavoriteView::resourceKey)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CHART", "orders-trend"),
                        org.assertj.core.groups.Tuple.tuple("DASHBOARD", "marketing-overview"));
        assertThat(service.listFavoriteResources(7L, "analyst", " Dashboard "))
                .extracting(BiResourceFavoriteView::resourceKey)
                .containsExactly("marketing-overview");
        assertThat(service.listFavoriteResources(7L, "operator", null))
                .extracting(BiResourceFavoriteView::resourceKey)
                .containsExactly("executive-overview");
        assertThat(service.listFavoriteResources(42L, "analyst", null))
                .extracting(BiResourceFavoriteView::resourceKey)
                .containsExactly("tenant-42-overview");

        service.unfavoriteResource(7L, "analyst", "dashboard", "marketing-overview");
        service.unfavoriteResource(7L, "analyst", "dashboard", "marketing-overview");

        assertThat(service.listFavoriteResources(7L, "analyst", null))
                .extracting(BiResourceFavoriteView::resourceKey)
                .containsExactly("orders-trend");
    }

    private static BiWorkspace workspace(Long id) {
        return new BiWorkspace(
                id,
                7L,
                BiResourceKey.of("Growth Team", "workspaceKey"),
                "Growth team",
                "Growth BI workspace",
                BiResourceStatus.PUBLISHED,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiDataset dataset(Long id, String key, BiResourceStatus status) {
        return dataset(id, key, status, LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiDataset dataset(Long id, String key, BiResourceStatus status, LocalDateTime updatedAt) {
        return new BiDataset(
                id,
                7L,
                5L,
                BiResourceKey.of(key, "datasetKey"),
                "Orders daily",
                "SQL",
                99L,
                "fact_order",
                "tenant_id",
                Map.of(),
                List.of(),
                List.of(),
                status,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                updatedAt);
    }

    private static BiChart chart(Long id, String key, BiResourceStatus status) {
        return chart(id, key, status, LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiChart chart(Long id, String key, BiResourceStatus status, LocalDateTime updatedAt) {
        return new BiChart(
                id,
                7L,
                5L,
                BiResourceKey.of(key, "chartKey"),
                "Orders trend",
                "line",
                100L,
                BiResourceKey.of("orders-daily", "datasetKey"),
                Map.of("dimensions", List.of("order_date")),
                Map.of("palette", "ops"),
                Map.of(),
                status,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                updatedAt);
    }

    private static BiDashboard dashboard(Long id, String key, BiResourceStatus status) {
        return dashboard(id, key, status, LocalDateTime.parse("2026-06-01T00:00:00"));
    }

    private static BiDashboard dashboard(Long id, String key, BiResourceStatus status, LocalDateTime updatedAt) {
        return new BiDashboard(
                id,
                7L,
                5L,
                BiResourceKey.of(key, "dashboardKey"),
                "Marketing overview",
                "Executive daily view",
                Map.of("mode", "light"),
                Map.of("region", "CN"),
                List.of("orders-trend"),
                status,
                1,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                updatedAt);
    }

    private static List<String> recordComponentNames(Class<? extends Record> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private static MybatisBiCatalogRepository repositoryWithDatasetMapper(BiDatasetMapper datasetMapper) {
        return repositoryWithMappers(mock(BiWorkspaceMapper.class), datasetMapper);
    }

    private static MybatisBiCatalogRepository repositoryWithChartMapper(BiWorkspaceMapper workspaceMapper,
                                                                        BiChartMapper chartMapper) {
        return new MybatisBiCatalogRepository(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class),
                chartMapper,
                mock(BiDashboardMapper.class),
                mock(BiDashboardWidgetMapper.class),
                mock(BiResourcePermissionMapper.class),
                new BiPersistenceConverter());
    }

    private static MybatisBiCatalogRepository repositoryWithDashboardMapper(BiWorkspaceMapper workspaceMapper,
                                                                            BiDashboardMapper dashboardMapper) {
        return new MybatisBiCatalogRepository(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class),
                mock(BiChartMapper.class),
                dashboardMapper,
                mock(BiDashboardWidgetMapper.class),
                mock(BiResourcePermissionMapper.class),
                new BiPersistenceConverter());
    }

    private static MybatisBiCatalogRepository repositoryWithMappers(BiWorkspaceMapper workspaceMapper,
                                                                    BiDatasetMapper datasetMapper) {
        return new MybatisBiCatalogRepository(
                workspaceMapper,
                datasetMapper,
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class),
                mock(BiChartMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiDashboardWidgetMapper.class),
                mock(BiResourcePermissionMapper.class),
                new BiPersistenceConverter());
    }

    private static void initBiDatasetTableInfo() {
        if (TableInfoHelper.getTableInfo(BiDatasetDO.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BiDatasetDO.class);
        }
    }

    private static void initBiWorkspaceTableInfo() {
        if (TableInfoHelper.getTableInfo(BiWorkspaceDO.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BiWorkspaceDO.class);
        }
    }

    private static void initBiChartTableInfo() {
        if (TableInfoHelper.getTableInfo(BiChartDO.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BiChartDO.class);
        }
    }

    private static void initBiDashboardTableInfo() {
        if (TableInfoHelper.getTableInfo(BiDashboardDO.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BiDashboardDO.class);
        }
    }

    private static BiWorkspaceDO workspaceRow(Long id, Long tenantId) {
        BiWorkspaceDO row = new BiWorkspaceDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setWorkspaceKey("marketing_canvas");
        row.setName("Marketing Canvas");
        row.setStatus("ACTIVE");
        row.setCreatedBy("admin");
        row.setCreatedAt(LocalDateTime.parse("2026-06-01T00:00:00"));
        row.setUpdatedAt(LocalDateTime.parse("2026-06-01T00:00:00"));
        return row;
    }

    private static BiDatasetDO datasetRow(Long id, Long tenantId, String datasetKey, BiResourceStatus status) {
        return datasetRow(id, tenantId, 5L, datasetKey, status);
    }

    private static BiDatasetDO datasetRow(Long id,
                                          Long tenantId,
                                          Long workspaceId,
                                          String datasetKey,
                                          BiResourceStatus status) {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetKey(datasetKey);
        row.setName("Orders daily");
        row.setDatasetType("SQL");
        row.setSourceRefId(99L);
        row.setTableExpression("fact_order");
        row.setTenantColumn("tenant_id");
        row.setModelJson("{}");
        row.setStatus(status.name());
        row.setCreatedBy("admin");
        row.setCreatedAt(LocalDateTime.parse("2026-06-01T00:00:00"));
        row.setUpdatedAt(LocalDateTime.parse("2026-06-01T00:00:00"));
        return row;
    }

    private static final class FakeRepository implements BiWorkspaceRepository, BiDatasetRepository,
            BiChartRepository, BiDashboardRepository, BiPermissionRepository {
        private final Map<Long, BiWorkspace> workspacesById = new LinkedHashMap<>();
        private final Map<Long, BiDataset> datasetsById = new LinkedHashMap<>();
        private final Map<Long, BiChart> chartsById = new LinkedHashMap<>();
        private final Map<Long, BiDashboard> dashboardsById = new LinkedHashMap<>();
        private final List<BiPermissionGrant> grants = new ArrayList<>();
        private long nextDatasetId = 100L;
        private long nextChartId = 200L;
        private long nextDashboardId = 300L;
        private long nextGrantId = 400L;

        @Override
        public BiWorkspace findWorkspace(Long tenantId, Long workspaceId) {
            BiWorkspace workspace = workspacesById.get(workspaceId);
            return workspace == null || !tenantId.equals(workspace.tenantId()) ? null : workspace;
        }

        @Override
        public BiWorkspace saveWorkspace(BiWorkspace workspace) {
            workspacesById.put(workspace.id(), workspace);
            return workspace;
        }

        @Override
        public BiDataset findDatasetByKey(Long tenantId, Long workspaceId, BiResourceKey datasetKey) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> workspaceId.equals(dataset.workspaceId()))
                    .filter(dataset -> dataset.datasetKey().equals(datasetKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDataset findDatasetById(Long tenantId, Long datasetId) {
            BiDataset dataset = datasetsById.get(datasetId);
            return dataset == null || !tenantId.equals(dataset.tenantId()) ? null : dataset;
        }

        @Override
        public List<BiDataset> listAvailableDatasets(Long tenantId) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> dataset.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.datasetKey().value().compareTo(right.datasetKey().value());
                    })
                    .toList();
        }

        @Override
        public BiDataset findAvailableDatasetByKeyWithTenantFallback(Long tenantId, BiResourceKey datasetKey) {
            BiDataset tenantDataset = findAvailableDatasetByKey(tenantId, datasetKey);
            return tenantDataset == null ? findAvailableDatasetByKey(0L, datasetKey) : tenantDataset;
        }

        private BiDataset findAvailableDatasetByKey(Long tenantId, BiResourceKey datasetKey) {
            return datasetsById.values().stream()
                    .filter(dataset -> tenantId.equals(dataset.tenantId()))
                    .filter(dataset -> dataset.datasetKey().equals(datasetKey))
                    .filter(dataset -> dataset.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDataset saveDataset(BiDataset dataset) {
            BiDataset saved = dataset.id() == null ? dataset.withId(nextDatasetId++) : dataset;
            datasetsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiChart findChartByKey(Long tenantId, Long workspaceId, BiResourceKey chartKey) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> workspaceId.equals(chart.workspaceId()))
                    .filter(chart -> chart.chartKey().equals(chartKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<BiChart> listChartsByKeys(Long tenantId, Long workspaceId, List<BiResourceKey> chartKeys) {
            return chartKeys.stream()
                    .map(key -> findChartByKey(tenantId, workspaceId, key))
                    .filter(chart -> chart != null)
                    .toList();
        }

        @Override
        public List<BiChart> listAvailableCharts(Long tenantId) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> chart.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.chartKey().value().compareTo(right.chartKey().value());
                    })
                    .toList();
        }

        @Override
        public BiChart findAvailableChartByKey(Long tenantId, BiResourceKey chartKey) {
            return chartsById.values().stream()
                    .filter(chart -> tenantId.equals(chart.tenantId()))
                    .filter(chart -> chart.chartKey().equals(chartKey))
                    .filter(chart -> chart.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiChart saveChart(BiChart chart) {
            BiChart saved = chart.id() == null ? chart.withId(nextChartId++) : chart;
            chartsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiDashboard findDashboardByKey(Long tenantId, Long workspaceId, BiResourceKey dashboardKey) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> workspaceId.equals(dashboard.workspaceId()))
                    .filter(dashboard -> dashboard.dashboardKey().equals(dashboardKey))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<BiDashboard> listAvailableDashboards(Long tenantId) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> dashboard.status() != BiResourceStatus.ARCHIVED)
                    .sorted((left, right) -> {
                        int updated = right.updatedAt().compareTo(left.updatedAt());
                        if (updated != 0) {
                            return updated;
                        }
                        return left.dashboardKey().value().compareTo(right.dashboardKey().value());
                    })
                    .toList();
        }

        @Override
        public BiDashboard findAvailableDashboardByKey(Long tenantId, BiResourceKey dashboardKey) {
            return dashboardsById.values().stream()
                    .filter(dashboard -> tenantId.equals(dashboard.tenantId()))
                    .filter(dashboard -> dashboard.dashboardKey().equals(dashboardKey))
                    .filter(dashboard -> dashboard.status() != BiResourceStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BiDashboard saveDashboard(BiDashboard dashboard) {
            BiDashboard saved = dashboard.id() == null ? dashboard.withId(nextDashboardId++) : dashboard;
            dashboardsById.put(saved.id(), saved);
            return saved;
        }

        @Override
        public BiPermissionGrant saveGrant(BiPermissionGrant grant) {
            BiPermissionGrant saved = grant.id() == null ? grant.withId(nextGrantId++) : grant;
            grants.add(saved);
            return saved;
        }

        @Override
        public List<BiPermissionGrant> listResourceGrants(Long tenantId,
                                                          Long workspaceId,
                                                          String resourceType,
                                                          Long resourceId) {
            return grants.stream()
                    .filter(grant -> tenantId.equals(grant.tenantId()))
                    .filter(grant -> workspaceId.equals(grant.workspaceId()))
                    .filter(grant -> resourceType.equals(grant.resourceType()))
                    .filter(grant -> resourceId.equals(grant.resourceId()))
                    .toList();
        }
    }
}
