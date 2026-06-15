package org.chovy.canvas.bi.application;

import org.chovy.canvas.bi.api.BiChartCommand;
import org.chovy.canvas.bi.api.BiChartReferenceImpactView;
import org.chovy.canvas.bi.api.BiChartView;
import org.chovy.canvas.bi.api.BiDashboardCommand;
import org.chovy.canvas.bi.api.BiDashboardCloneCommand;
import org.chovy.canvas.bi.api.BiDashboardExportPackageView;
import org.chovy.canvas.bi.api.BiDashboardImportCommand;
import org.chovy.canvas.bi.api.BiDashboardPresetView;
import org.chovy.canvas.bi.api.BiDashboardReadModelView;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateCommand;
import org.chovy.canvas.bi.api.BiDashboardRuntimeStateView;
import org.chovy.canvas.bi.api.BiDashboardView;
import org.chovy.canvas.bi.api.BiDatasetCommand;
import org.chovy.canvas.bi.api.BiDatasetFieldCommand;
import org.chovy.canvas.bi.api.BiDatasetView;
import org.chovy.canvas.bi.api.BiMetricCommand;
import org.chovy.canvas.bi.api.BiAlertRuleCommand;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentDownload;
import org.chovy.canvas.bi.api.BiDatasourceApiPreviewCommand;
import org.chovy.canvas.bi.api.BiDatasourceFileMaterializationResult;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingCommand;
import org.chovy.canvas.bi.api.BiDatasourceOnboardingView;
import org.chovy.canvas.bi.api.BiDatasourceSchemaSnapshotView;
import org.chovy.canvas.bi.api.BiSelfServiceExportCommand;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobView;
import org.chovy.canvas.bi.api.BiSelfServiceExportReviewCommand;
import org.chovy.canvas.bi.api.BiSelfServicePreviewCommand;
import org.chovy.canvas.bi.api.BiSubscriptionCommand;
import org.chovy.canvas.bi.api.BiPermissionDecisionView;
import org.chovy.canvas.bi.api.BiPermissionGrantCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestReviewCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestView;
import org.chovy.canvas.bi.api.BiResourcePermissionCommand;
import org.chovy.canvas.bi.api.BiResourcePermissionView;
import org.chovy.canvas.bi.api.BiRowPermissionCommand;
import org.chovy.canvas.bi.api.BiBigScreenResourceCommand;
import org.chovy.canvas.bi.api.BiBigScreenResourceView;
import org.chovy.canvas.bi.api.BiAiRequestCommand;
import org.chovy.canvas.bi.api.BiAiResponseView;
import org.chovy.canvas.bi.api.BiPortalResourceCommand;
import org.chovy.canvas.bi.api.BiPortalResourceView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;
import org.chovy.canvas.bi.api.BiQueryDatasetView;
import org.chovy.canvas.bi.api.BiQueryGateCommand;
import org.chovy.canvas.bi.api.BiQueryCommand;
import org.chovy.canvas.bi.api.BiQueryResultView;
import org.chovy.canvas.bi.api.BiQueryCachePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketVerifyCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketPayloadView;
import org.chovy.canvas.bi.api.BiEmbedQueryCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalReviewCommand;
import org.chovy.canvas.bi.api.BiPublishApprovalView;
import org.chovy.canvas.bi.api.BiResourceVersionView;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceView;
import org.chovy.canvas.bi.api.BiResourceCommentCommand;
import org.chovy.canvas.bi.api.BiResourceCommentView;
import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;
import org.chovy.canvas.bi.api.BiResourceLocationCommand;
import org.chovy.canvas.bi.api.BiResourceLocationView;
import org.chovy.canvas.bi.api.BiResourceLockCommand;
import org.chovy.canvas.bi.api.BiResourceLockView;
import org.chovy.canvas.bi.api.BiResourceMoveCommand;
import org.chovy.canvas.bi.api.BiResourceOwnershipView;
import org.chovy.canvas.bi.api.BiResourceTransferCommand;
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
    void selfServiceExportCatalogScopesPreviewExportsAndLifecycleByTenant() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        Map<String, Object> preview = service.previewSelfServiceExport(7L, "analyst", "ANALYST",
                new BiSelfServicePreviewCommand(Map.of(
                        "datasetKey", "orders_daily",
                        "dimensions", List.of("stat_date"),
                        "metrics", List.of("gmv")), 2));

        assertThat(preview)
                .containsEntry("datasetKey", "orders_daily")
                .containsEntry("rowCount", 2);
        assertThat((List<?>) preview.get("rows")).hasSize(2);

        BiSelfServiceExportJobView pending = service.createSelfServiceExport(7L, "analyst", "ANALYST",
                new BiSelfServiceExportCommand(
                        "dashboard",
                        "Marketing Overview",
                        300L,
                        "csv",
                        Map.of("datasetKey", "orders_daily"),
                        500,
                        true,
                        false,
                        "monthly close"));

        assertThat(pending.tenantId()).isEqualTo(7L);
        assertThat(pending.status()).isEqualTo("PENDING_REVIEW");
        assertThat(service.listSelfServiceExports(8L, 20)).isEmpty();

        BiSelfServiceExportJobView approved = service.reviewSelfServiceExport(7L, "lead", "ADMIN", pending.id(),
                new BiSelfServiceExportReviewCommand("approved", "ship it"));
        BiSelfServiceExportJobView completed = service.runSelfServiceExportQueue(7L, "worker", "ADMIN", 10)
                .jobs()
                .get(0);

        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(service.getSelfServiceExportDetail(7L, pending.id()).partition())
                .containsEntry("tenantId", 7L);
        assertThat(service.downloadSelfServiceExport(7L, "analyst", pending.id()).filename())
                .isEqualTo("bi-export-" + pending.id() + ".csv");
        assertThat(service.cancelSelfServiceExport(7L, "analyst", pending.id()).status())
                .isEqualTo("CANCELLED");
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
    void resourceOperationsCatalogScopesByTenantNormalizesKeysAndKeepsDeterministicOrdering() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiResourceCommentView firstComment = service.addResourceComment(7L, new BiResourceCommentCommand(
                " Dashboard ",
                " Marketing Overview ",
                " Revenue Widget ",
                "Looks good"), " analyst ");
        BiResourceCommentView secondComment = service.addResourceComment(7L, new BiResourceCommentCommand(
                "chart",
                "Orders Trend",
                null,
                "Check trend"), "operator");
        service.addResourceComment(42L, new BiResourceCommentCommand(
                "dashboard",
                "Marketing Overview",
                null,
                "Tenant scoped"), "other");

        List<BiResourceCommentView> comments = service.listResourceComments(7L, " DASHBOARD ", " Marketing Overview ");

        assertThat(firstComment.tenantId()).isEqualTo(7L);
        assertThat(firstComment.resourceType()).isEqualTo("DASHBOARD");
        assertThat(firstComment.resourceKey()).isEqualTo("marketing-overview");
        assertThat(firstComment.widgetKey()).isEqualTo("revenue-widget");
        assertThat(firstComment.createdBy()).isEqualTo("analyst");
        assertThat(comments).extracting(BiResourceCommentView::id).containsExactly(firstComment.id());
        assertThat(service.listResourceComments(7L, null, null))
                .extracting(BiResourceCommentView::id)
                .containsExactly(firstComment.id(), secondComment.id());

        BiResourceLocationView location = service.updateResourceLocation(7L, new BiResourceLocationCommand(
                "dashboard",
                "Marketing Overview",
                " Executive / Weekly ",
                20), "operator");
        service.moveResource(7L, new BiResourceMoveCommand(
                "chart",
                "Orders Trend",
                "Executive",
                10), "operator");

        assertThat(location.resourceType()).isEqualTo("DASHBOARD");
        assertThat(location.resourceKey()).isEqualTo("marketing-overview");
        assertThat(location.folderKey()).isEqualTo("executive/weekly");
        assertThat(service.listResourceLocations(7L, null))
                .extracting(BiResourceLocationView::resourceType, BiResourceLocationView::resourceKey)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CHART", "orders-trend"),
                        org.assertj.core.groups.Tuple.tuple("DASHBOARD", "marketing-overview"));
    }

    @Test
    void resourceOperationsCatalogMakesDeleteAndReleaseIdempotentAndModelsApprovals() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiResourceCommentView comment = service.addResourceComment(7L, new BiResourceCommentCommand(
                "dashboard",
                "Marketing Overview",
                null,
                "Ready"), "analyst");
        service.deleteResourceComment(7L, "analyst", comment.id());
        service.deleteResourceComment(7L, "analyst", comment.id());

        assertThat(service.listResourceComments(7L, "dashboard", "marketing-overview")).isEmpty();

        BiResourceLockView lock = service.acquireResourceLock(7L, new BiResourceLockCommand(
                " dashboard ",
                " Marketing Overview ",
                "token-1",
                120), " analyst ");

        assertThat(lock.locked()).isTrue();
        assertThat(lock.lockToken()).isEqualTo("token-1");
        assertThat(lock.expiresAt()).isEqualTo(LocalDateTime.parse("2026-06-06T10:02:00"));
        assertThat(service.currentResourceLock(7L, "dashboard", "marketing-overview").locked()).isTrue();

        service.releaseResourceLock(7L, "analyst", new BiResourceLockCommand(
                "dashboard",
                "marketing-overview",
                "token-1",
                null));
        service.releaseResourceLock(7L, "analyst", new BiResourceLockCommand(
                "dashboard",
                "marketing-overview",
                "token-1",
                null));

        assertThat(service.currentResourceLock(7L, "dashboard", "marketing-overview").locked()).isFalse();

        BiResourceOwnershipView ownership = service.transferResource(7L, new BiResourceTransferCommand(
                "dashboard",
                "Marketing Overview",
                "bi-owner"), "operator");
        assertThat(ownership.ownerUser()).isEqualTo("bi-owner");
        assertThat(service.listResourceOwnerships(7L, "dashboard")).singleElement()
                .satisfies(view -> assertThat(view.resourceKey()).isEqualTo("marketing-overview"));

        BiPublishApprovalView approval = service.requestPublishApproval(7L, new BiPublishApprovalCommand(
                "dashboard",
                "Marketing Overview",
                "Ready for release"), "analyst");
        BiPublishApprovalView reviewed = service.reviewPublishApproval(7L, new BiPublishApprovalReviewCommand(
                approval.id(),
                " approved ",
                "ship it"), "lead");

        assertThat(approval.status()).isEqualTo("PENDING");
        assertThat(reviewed.status()).isEqualTo("APPROVED");
        assertThat(reviewed.reviewedBy()).isEqualTo("lead");
        assertThat(service.listPublishApprovals(7L, "dashboard", "Marketing Overview", "approved")).singleElement()
                .satisfies(view -> assertThat(view.id()).isEqualTo(approval.id()));
    }

    @Test
    void portalResourceLifecycleScopesByTenantNormalizesKeysAndRestoresVersions() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiPortalResourceView draft = service.savePortalDraft(7L, " Marketing Portal ", new BiPortalResourceCommand(
                "Body key loses",
                "Marketing portal",
                "Executive BI portal",
                List.of(" Marketing Overview ", "Orders Trend"),
                Map.of("columns", 12),
                Map.of("theme", "light"),
                null), " analyst ");
        BiPortalResourceView published = service.publishPortalResource(7L, "marketing-portal", "publisher");
        service.savePortalDraft(42L, "Marketing Portal", new BiPortalResourceCommand(
                null,
                "Other tenant",
                "Tenant scoped",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");
        service.savePortalDraft(7L, "Another Portal", new BiPortalResourceCommand(
                null,
                "Another portal",
                "Later updated resource",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");

        BiPortalResourceView restored = service.restorePortalResourceVersion(7L, "marketing-portal", 1, "restorer");

        assertThat(draft.tenantId()).isEqualTo(7L);
        assertThat(draft.portalKey()).isEqualTo("marketing-portal");
        assertThat(draft.dashboardKeys()).containsExactly("marketing-overview", "orders-trend");
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.version()).isEqualTo(1);
        assertThat(draft.createdBy()).isEqualTo("analyst");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(published.version()).isEqualTo(2);
        assertThat(restored.status()).isEqualTo("DRAFT");
        assertThat(restored.version()).isEqualTo(3);
        assertThat(service.listPortalResources(7L)).extracting(BiPortalResourceView::portalKey)
                .containsExactly("another-portal", "marketing-portal");
        assertThat(service.getPortalResource(7L, " Marketing Portal ").portalKey()).isEqualTo("marketing-portal");
        assertThat(service.listPortalResourceVersions(7L, "marketing-portal"))
                .extracting(BiResourceVersionView::version)
                .containsExactly(3, 2, 1);

        service.archivePortalResource(7L, "marketing-portal", "archiver");
        service.archivePortalResource(7L, "marketing-portal", "archiver");

        assertThat(service.listPortalResources(7L)).extracting(BiPortalResourceView::portalKey)
                .containsExactly("another-portal");
        assertThatThrownBy(() -> service.getPortalResource(7L, "marketing-portal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI portal resource not found");
    }

    @Test
    void bigScreenResourceLifecycleScopesByTenantNormalizesKeysAndRestoresVersions() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiBigScreenResourceView draft = service.saveBigScreenDraft(7L, " Revenue Wall ", new BiBigScreenResourceCommand(
                "body key loses",
                "Revenue wall",
                "Command center",
                List.of(" Marketing Overview "),
                Map.of("resolution", "1920x1080"),
                Map.of("refreshSeconds", 60),
                null), " analyst ");
        BiBigScreenResourceView published = service.publishBigScreenResource(7L, "revenue-wall", "publisher");
        service.saveBigScreenDraft(42L, "Revenue Wall", new BiBigScreenResourceCommand(
                null,
                "Other tenant",
                "Tenant scoped",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");
        service.saveBigScreenDraft(7L, "Ops Wall", new BiBigScreenResourceCommand(
                null,
                "Ops wall",
                "Later updated resource",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");

        BiBigScreenResourceView restored = service.restoreBigScreenResourceVersion(7L, "revenue-wall", 1, "restorer");

        assertThat(draft.tenantId()).isEqualTo(7L);
        assertThat(draft.screenKey()).isEqualTo("revenue-wall");
        assertThat(draft.dashboardKeys()).containsExactly("marketing-overview");
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.version()).isEqualTo(1);
        assertThat(draft.createdBy()).isEqualTo("analyst");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(published.version()).isEqualTo(2);
        assertThat(restored.status()).isEqualTo("DRAFT");
        assertThat(restored.version()).isEqualTo(3);
        assertThat(service.listBigScreenResources(7L)).extracting(BiBigScreenResourceView::screenKey)
                .containsExactly("ops-wall", "revenue-wall");
        assertThat(service.getBigScreenResource(7L, " Revenue Wall ").screenKey()).isEqualTo("revenue-wall");
        assertThat(service.listBigScreenResourceVersions(7L, "revenue-wall"))
                .extracting(BiResourceVersionView::version)
                .containsExactly(3, 2, 1);

        service.archiveBigScreenResource(7L, "revenue-wall", "archiver");
        service.archiveBigScreenResource(7L, "revenue-wall", "archiver");

        assertThat(service.listBigScreenResources(7L)).extracting(BiBigScreenResourceView::screenKey)
                .containsExactly("ops-wall");
        assertThatThrownBy(() -> service.getBigScreenResource(7L, "revenue-wall"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI big-screen resource not found");
    }

    @Test
    void spreadsheetResourceLifecycleScopesByTenantNormalizesKeysAndRestoresVersions() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiSpreadsheetResourceView draft = service.saveSpreadsheetDraft(7L, " Revenue Sheet ",
                new BiSpreadsheetResourceCommand(
                        "body key loses",
                        "Revenue sheet",
                        "Finance workbook",
                        List.of(Map.of("sheetKey", " Daily Revenue ", "title", "Daily revenue")),
                        Map.of("datasetKey", " Orders Daily "),
                        Map.of("theme", "compact"),
                        null), " analyst ");
        BiSpreadsheetResourceView published = service.publishSpreadsheetResource(7L, "revenue-sheet", "publisher");
        service.saveSpreadsheetDraft(42L, "Revenue Sheet", new BiSpreadsheetResourceCommand(
                null,
                "Other tenant",
                "Tenant scoped",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");
        service.saveSpreadsheetDraft(7L, "Ops Sheet", new BiSpreadsheetResourceCommand(
                null,
                "Ops sheet",
                "Later updated resource",
                List.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");

        BiSpreadsheetResourceView restored = service.restoreSpreadsheetResourceVersion(
                7L,
                "revenue-sheet",
                1,
                "restorer");

        assertThat(draft.tenantId()).isEqualTo(7L);
        assertThat(draft.spreadsheetKey()).isEqualTo("revenue-sheet");
        assertThat(draft.sheets()).singleElement()
                .satisfies(sheet -> assertThat(sheet).containsEntry("sheetKey", "Daily Revenue"));
        assertThat(draft.dataBinding()).containsEntry("datasetKey", " Orders Daily ");
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.version()).isEqualTo(1);
        assertThat(draft.createdBy()).isEqualTo("analyst");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(published.version()).isEqualTo(2);
        assertThat(restored.status()).isEqualTo("DRAFT");
        assertThat(restored.version()).isEqualTo(3);
        assertThat(service.listSpreadsheetResources(7L)).extracting(BiSpreadsheetResourceView::spreadsheetKey)
                .containsExactly("ops-sheet", "revenue-sheet");
        assertThat(service.getSpreadsheetResource(7L, " Revenue Sheet ").spreadsheetKey())
                .isEqualTo("revenue-sheet");
        assertThat(service.listSpreadsheetResourceVersions(7L, "revenue-sheet"))
                .extracting(BiResourceVersionView::version)
                .containsExactly(3, 2, 1);

        service.archiveSpreadsheetResource(7L, "revenue-sheet", "archiver");
        service.archiveSpreadsheetResource(7L, "revenue-sheet", "archiver");

        assertThat(service.listSpreadsheetResources(7L)).extracting(BiSpreadsheetResourceView::spreadsheetKey)
                .containsExactly("ops-sheet");
        assertThatThrownBy(() -> service.getSpreadsheetResource(7L, "revenue-sheet"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI spreadsheet resource not found");
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
    void queryOperationsCatalogCompilesExecutesTracksHistoryAndDefaultsPoliciesByTenant() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiQueryCommand command = new BiQueryCommand(
                " Canvas Daily Stats ",
                " Marketing Overview ",
                List.of(" stat_date "),
                List.of(" total_executions "),
                List.of(Map.of("field", "trigger_type", "op", "EQ", "value", "AUTO")),
                List.of(Map.of("field", "stat_date", "direction", "DESC")),
                3,
                0,
                Map.of("region", "CN"));

        String sql = service.compileQuery(7L, command, " analyst ").sql();
        BiQueryResultView result = service.executeQuery(7L, command, " analyst ");
        service.executeGatedQuery(7L, new BiQueryGateCommand(command, null, null, "online", false), "analyst");

        assertThat(sql).contains("canvas_daily_stats", "tenant_id = ?", "LIMIT 3");
        assertThat(result.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(result.columns()).extracting(column -> column.get("fieldKey"))
                .containsExactly("stat_date", "total_executions");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.sqlHash()).startsWith("biq-");
        assertThat(service.explainQuery(7L, command, "analyst").steps()).anySatisfy(step ->
                assertThat(step).contains("canvas_daily_stats"));
        assertThat(service.listQueryHistory(7L, 10))
                .extracting("status")
                .containsExactly("SUCCESS", "SUCCESS");
        assertThat(service.queryHistoryDetail(7L, 1L).request().datasetKey()).isEqualTo(" Canvas Daily Stats ");
        assertThat(service.cancelQuery(7L, result.sqlHash(), "operator").cancelled()).isTrue();
        assertThat(service.cancelQuery(7L, result.sqlHash(), "operator").status()).isEqualTo("CANCELLED");
        assertThat(service.queryGovernanceSummary(7L, 10).totalQueries()).isEqualTo(2);
        assertThat(service.queryGovernancePolicy(7L).defaultTimeoutMs()).isEqualTo(30000L);
        assertThat(service.updateQueryCachePolicy(7L, new BiQueryCachePolicyCommand(
                false,
                60L,
                "direct_query",
                List.of(Map.of("resourceType", "dataset", "resourceKey", "Canvas Daily Stats", "enabled", true))),
                "admin").defaultCacheMode()).isEqualTo("DIRECT_QUERY");
        assertThat(service.invalidateQueryCache(7L, new BiQueryCacheInvalidationCommand(
                " Canvas Daily Stats ",
                null,
                result.sqlHash())).status()).isEqualTo("INVALIDATED");
        assertThat(service.queryCacheStats(7L).provider()).isEqualTo("final-bi-memory");
        assertThat(service.datasourceHealth()).isEmpty();
        assertThat(service.datasourceHealthSlo(10).availabilityRate()).isEqualTo(100.0);
    }

    @Test
    void datasourceOperationsCatalogScopesByTenantAndSupportsPreviewRotationAndSnapshots() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiDatasourceOnboardingView source = service.createDatasource(7L, new BiDatasourceOnboardingCommand(
                " mysql ",
                " Orders Warehouse ",
                "jdbc:mysql://db.internal:3306/orders",
                "report_user",
                "secret",
                null,
                "Primary warehouse",
                true,
                null,
                Map.of("schema", "dw")), " analyst ");
        service.createDatasource(42L, new BiDatasourceOnboardingCommand(
                "mysql",
                "Other Tenant Warehouse",
                "jdbc:mysql://other",
                "other",
                "secret",
                null,
                null,
                true,
                null,
                Map.of()), "operator");

        assertThat(service.datasourceConnectors()).extracting("connectorType")
                .containsExactly("API_JSON", "FILE_CSV", "MYSQL", "POSTGRESQL");
        assertThat(source.tenantId()).isEqualTo(7L);
        assertThat(source.sourceKey()).isEqualTo("orders-warehouse");
        assertThat(source.connectorType()).isEqualTo("MYSQL");
        assertThat(source.maskedUrl()).isEqualTo("jdbc:mysql://db.internal:****/orders");
        assertThat(source.maskedUsername()).isEqualTo("r***r");
        assertThat(service.listDatasources(7L)).extracting(BiDatasourceOnboardingView::sourceKey)
                .containsExactly("orders-warehouse");
        assertThat(service.testDatasourceConnection(7L, source.id()).success()).isTrue();
        assertThat(service.rotateDatasourceCredential(7L, source.id(), null, " operator ").rotatedBy())
                .isEqualTo("operator");
        assertThat(service.previewDatasourceSchema(7L, source.id(), 10).tables()).singleElement()
                .satisfies(table -> assertThat(table).containsEntry("tableName", "orders_warehouse_sample"));
        assertThat(service.previewDatasourceApi(7L, source.id(), new BiDatasourceApiPreviewCommand(
                Map.of("region", "CN"),
                "/orders",
                "POST",
                Map.of("X-Env", "test"),
                Map.of("page", 1),
                2)).rows()).hasSize(1);

        BiDatasourceSchemaSnapshotView snapshot = service.syncDatasourceSchema(7L, source.id(), 10, null, " syncer ");

        assertThat(snapshot.syncStatus()).isEqualTo("SUCCESS");
        assertThat(snapshot.syncedBy()).isEqualTo("syncer");
        assertThat(service.latestDatasourceSchemaSnapshot(7L, source.id()).id()).isEqualTo(snapshot.id());
        assertThat(service.listDatasourceSchemaSnapshots(7L, source.id(), 20))
                .extracting(BiDatasourceSchemaSnapshotView::id)
                .containsExactly(snapshot.id());
    }

    @Test
    void datasourceFileUploadCreatesCompactSourcesWithoutPersistingBytesAndMaterializesDatasetShape() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiDatasourceOnboardingView uploaded = service.uploadDatasourceFile(
                7L,
                " analyst ",
                "orders.csv",
                " Orders CSV ",
                "Daily export",
                null,
                ",",
                true,
                "UTF-8");
        BiDatasourceFileMaterializationResult materialized = service.materializeDatasourceFile(
                7L,
                " analyst ",
                "orders.csv",
                " Orders CSV ",
                "Daily export",
                null,
                ",",
                true,
                "UTF-8",
                " Orders Daily ",
                "Orders daily",
                "tenant_id",
                200,
                1000L);

        assertThat(uploaded.connectorType()).isEqualTo("FILE_CSV");
        assertThat(uploaded.sourceKey()).isEqualTo("orders-csv");
        assertThat(materialized.source().sourceKey()).isEqualTo("orders-csv");
        assertThat(materialized.schemaSnapshot().tableCount()).isEqualTo(1);
        assertThat(materialized.dataset())
                .containsEntry("datasetKey", "orders-daily")
                .containsEntry("tenantColumn", "tenant_id");
        assertThat(materialized.refreshRun())
                .containsEntry("status", "SUCCESS")
                .containsEntry("importedRows", 1000L);
    }

    @Test
    void embedTicketOperationsNormalizeScopeEnforceOriginAndExecuteScopedQueryOnce() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        String ticket = service.createEmbedTicket(7L, new BiEmbedTicketCommand(
                " dashboard ",
                " Marketing Overview ",
                "view",
                Map.of("country", "CN"),
                120,
                List.of("https://example.com"),
                Map.of("menuDashboards", "marketing-overview,finance-overview"),
                2,
                60), " analyst ").ticket();
        BiEmbedTicketPayloadView payload = service.verifyEmbedTicket(
                new BiEmbedTicketVerifyCommand(ticket),
                "https://example.com/bi");
        BiQueryResultView result = service.executeEmbedQuery(new BiEmbedQueryCommand(
                ticket,
                "dashboard",
                "marketing-overview",
                "trend",
                new BiQueryCommand("canvas_daily_stats", "marketing-overview", List.of("stat_date"),
                        List.of("total_executions"), List.of(), List.of(), 5, 0, Map.of())),
                "https://example.com");

        assertThat(payload.tenantId()).isEqualTo(7L);
        assertThat(payload.username()).isEqualTo("analyst");
        assertThat(payload.resourceType()).isEqualTo("DASHBOARD");
        assertThat(payload.resourceKey()).isEqualTo("marketing-overview");
        assertThat(result.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThatThrownBy(() -> service.verifyEmbedTicket(
                new BiEmbedTicketVerifyCommand(ticket),
                "https://evil.example"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("origin is not allowed");
        assertThat(service.cleanupEmbedTickets(7L, 100).checked()).isGreaterThanOrEqualTo(1);
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
    void aiAssistantCatalogScopesByTenantNormalizesOperationAndPreservesRequestHints() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiAiResponseView ask = service.aiAssistant(7L, " ask-data ", new BiAiRequestCommand(
                "Why did GMV move?",
                null,
                " Orders Daily ",
                null,
                null,
                null,
                null,
                10,
                99L,
                88L,
                "gpt-final",
                1500,
                Map.of("region", "CN"),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()), " analyst ");
        BiAiResponseView tenantScoped = service.aiAssistant(42L, "ask", new BiAiRequestCommand(
                "Why did GMV move?",
                null,
                "Orders Daily",
                null,
                null,
                null,
                null,
                10,
                null,
                null,
                null,
                null,
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()), "operator");

        assertThat(ask.tenantId()).isEqualTo(7L);
        assertThat(ask.actor()).isEqualTo("analyst");
        assertThat(ask.operation()).isEqualTo("ASK");
        assertThat(ask.assistantRunId()).isEqualTo("bi-ai-7-ask");
        assertThat(ask.question()).isEqualTo("Why did GMV move?");
        assertThat(ask.status()).isEqualTo("READY");
        assertThat(ask.fallbackUsed()).isTrue();
        assertThat(ask.explanation()).contains("orders-daily");
        assertThat(ask.metadata())
                .containsEntry("datasetKey", "orders-daily")
                .containsEntry("modelKey", "gpt-final")
                .containsEntry("limit", 10)
                .containsEntry("providerId", 99L)
                .containsEntry("templateId", 88L)
                .containsEntry("timeoutMs", 1500)
                .containsEntry("params", Map.of("region", "CN"));
        assertThat(tenantScoped.assistantRunId()).isEqualTo("bi-ai-42-ask");
    }

    @Test
    void aiAssistantCatalogReturnsRouteSpecificStableFields() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiAiResponseView interpretation = service.aiAssistant(7L, "interpret", new BiAiRequestCommand(
                "Explain the trend",
                null,
                null,
                " dashboard ",
                " Marketing Overview ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of("grain", "day"),
                List.of(),
                Map.of("dimensions", List.of("stat_date")),
                Map.of("rows", 3),
                Map.of(),
                Map.of()), "analyst");
        BiAiResponseView report = service.aiAssistant(7L, "report", new BiAiRequestCommand(
                null,
                null,
                null,
                null,
                null,
                "weekly",
                "Weekly BI Report",
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                List.of(Map.of("title", "Revenue", "body", "GMV moved up")),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()), "analyst");
        BiAiResponseView draft = service.aiAssistant(7L, "dashboard-draft", new BiAiRequestCommand(
                null,
                "Draft a revenue dashboard",
                "orders-daily",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of("theme", "light"),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()), "analyst");
        BiAiResponseView insights = service.aiAssistant(7L, "insights", new BiAiRequestCommand(
                "Find anomalies",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                List.of(),
                Map.of("metric", "gmv"),
                Map.of("rows", 2),
                Map.of("rows", 2),
                Map.of("rows", 2)), "analyst");

        assertThat(interpretation.summary()).contains("marketing-overview");
        assertThat(interpretation.keyFindings()).containsExactly("Subject DASHBOARD/marketing-overview is ready for BI review");
        assertThat(interpretation.recommendations()).containsExactly("Validate the generated interpretation against source dashboards");
        assertThat(report.title()).isEqualTo("Weekly BI Report");
        assertThat(report.sections()).containsExactly(Map.of("title", "Revenue", "body", "GMV moved up"));
        assertThat(report.nextActions()).containsExactly("Review report narrative", "Attach approved dashboard evidence");
        assertThat(draft.dashboard()).containsEntry("dashboardKey", "ai-draft-orders-daily");
        assertThat(draft.charts()).extracting(chart -> chart.get("chartKey")).containsExactly("ai-draft-orders-daily-trend");
        assertThat(draft.explanation()).contains("Draft a revenue dashboard");
        assertThat(insights.trends()).containsExactly("Current result is prepared for trend review");
        assertThat(insights.anomalies()).containsExactly("No deterministic anomaly detected in compact mode");
        assertThat(insights.opportunities()).containsExactly("Use the insight as a draft before publishing BI decisions");
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
    void chartLifecyclePublishesArchivesListsVersionsNewestFirstAndRestoresDraftSnapshot() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveWorkspace(workspace(5L));
        repository.saveDataset(dataset(100L, "orders-daily", BiResourceStatus.PUBLISHED));

        BiChartView draft = service.upsertChart(7L, new BiChartCommand(
                5L,
                " Orders Trend ",
                "Orders trend",
                "line",
                "orders-daily",
                Map.of("dimensions", List.of("order_date")),
                Map.of("palette", "ops"),
                Map.of("drilldown", true),
                "draft"), " analyst ");
        BiChartView published = service.publishChartResource(7L, "Orders Trend", " publisher ");
        repository.saveWorkspace(new BiWorkspace(
                6L,
                42L,
                BiResourceKey.of("Tenant 42", "workspaceKey"),
                "Tenant 42",
                "Tenant 42 BI workspace",
                BiResourceStatus.PUBLISHED,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")));
        repository.saveDataset(new BiDataset(
                101L,
                42L,
                6L,
                BiResourceKey.of("orders-daily", "datasetKey"),
                "Tenant 42 orders",
                "SQL",
                99L,
                "fact_order",
                "tenant_id",
                Map.of(),
                List.of(),
                List.of(),
                BiResourceStatus.PUBLISHED,
                "admin",
                LocalDateTime.parse("2026-06-01T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")));
        service.upsertChart(42L, new BiChartCommand(
                6L,
                "Orders Trend",
                "Tenant scoped",
                "bar",
                "orders-daily",
                Map.of(),
                Map.of(),
                Map.of(),
                "draft"), "operator");
        service.upsertChart(7L, new BiChartCommand(
                5L,
                "Orders Trend",
                "Orders trend v2",
                "bar",
                "orders-daily",
                Map.of("dimensions", List.of("region")),
                Map.of("palette", "growth"),
                Map.of(),
                "draft"), "editor");

        BiChartView restored = service.restoreChartResourceVersion(7L, "orders-trend", 1, " restorer ");

        assertThat(draft.chartKey()).isEqualTo("orders-trend");
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(repository.findAvailableChartByKey(7L, BiResourceKey.of("orders-trend", "chartKey")).status())
                .isEqualTo(BiResourceStatus.DRAFT);
        assertThat(restored.status()).isEqualTo("DRAFT");
        assertThat(restored.chartType()).isEqualTo("LINE");
        assertThat(restored.query()).containsEntry("dimensions", List.of("order_date"));
        assertThat(restored.style()).containsEntry("palette", "ops");
        assertThat(service.listChartResourceVersions(7L, "orders-trend"))
                .extracting(BiResourceVersionView::version)
                .containsExactly(4, 3, 2, 1);
        assertThat(service.listChartResourceVersions(7L, "orders-trend").getFirst().status()).isEqualTo("DRAFT");

        service.archiveChartResource(7L, "orders-trend", "archiver");
        service.archiveChartResource(7L, "orders-trend", "archiver");

        assertThat(service.listChartResources(7L)).extracting(BiChartView::chartKey)
                .doesNotContain("orders-trend");
        assertThatThrownBy(() -> service.getChartResource(7L, "orders-trend"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BI chart not found");
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
    void dashboardResourceOperationsCloneExportImportArchiveVersionsAndRuntimeStateByTenant() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);
        repository.saveWorkspace(workspace(5L));
        repository.saveDashboard(dashboard(300L, "marketing-overview", BiResourceStatus.PUBLISHED));

        BiDashboardView cloned = service.cloneDashboardResource(7L, "lead", "marketing-overview",
                new BiDashboardCloneCommand("marketing-copy", "Marketing copy", "Copied view"));

        assertThat(cloned.dashboardKey()).isEqualTo("marketing-copy");
        assertThat(cloned.name()).isEqualTo("Marketing copy");
        assertThat(cloned.status()).isEqualTo("DRAFT");
        assertThat(service.listDashboardResources(8L)).isEmpty();

        BiDashboardExportPackageView exported = service.exportDashboardResource(7L, "analyst", "marketing-copy");

        assertThat(exported.resourceType()).isEqualTo("DASHBOARD");
        assertThat(exported.sourceDashboardKey()).isEqualTo("marketing-copy");
        assertThat(exported.dashboard().chartKeys()).containsExactly("orders-trend");
        assertThat(service.exportDashboardResourceFile(7L, "analyst", "marketing-copy").filename())
                .isEqualTo("marketing-copy-v2.bi-dashboard.json");

        BiDashboardView imported = service.importDashboardResource(7L, "importer",
                new BiDashboardImportCommand(exported, "marketing-imported", "Marketing imported", false));

        assertThat(imported.dashboardKey()).isEqualTo("marketing-imported");
        assertThat(imported.name()).isEqualTo("Marketing imported");

        List<BiResourceVersionView> versions = service.listDashboardResourceVersions(7L, "marketing-copy", 20);
        assertThat(versions).extracting(BiResourceVersionView::version).contains(2, 1);

        service.upsertDashboard(7L, new BiDashboardCommand(
                5L,
                "marketing-copy",
                "Marketing changed",
                "Changed",
                Map.of("mode", "dark"),
                Map.of("region", "US"),
                List.of("orders-trend"),
                "draft"), "editor");

        BiDashboardView restored = service.restoreDashboardResourceVersion(7L, "marketing-copy", 1, "restorer");
        assertThat(restored.name()).isEqualTo("Marketing overview");
        assertThat(restored.status()).isEqualTo("DRAFT");

        BiDashboardRuntimeStateView emptyState = service.getDashboardRuntimeState(7L, "analyst", "marketing-copy");
        assertThat(emptyState.parameters()).isEmpty();
        BiDashboardRuntimeStateView savedState = service.saveDashboardRuntimeState(7L, "analyst", "marketing-copy",
                new BiDashboardRuntimeStateCommand(Map.of("range", "LAST_7_DAYS")));
        assertThat(savedState.parameters()).containsEntry("range", "LAST_7_DAYS");
        assertThat(service.getDashboardRuntimeState(7L, "analyst", "marketing-copy").parameters())
                .containsEntry("range", "LAST_7_DAYS");
        assertThat(service.getDashboardRuntimeState(8L, "analyst", "marketing-copy").parameters()).isEmpty();

        BiDashboardView archived = service.archiveDashboardResource(7L, "marketing-copy", "archiver");
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThatThrownBy(() -> service.getDashboardResource(7L, "marketing-copy"))
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

    @Test
    void permissionAdministrationCatalogScopesNormalizesFiltersOrdersAuditsAndReviewsRequests() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiResourcePermissionView resource = service.upsertResourcePermission(7L, " analyst ",
                new BiResourcePermissionCommand(
                        " dashboard ",
                        " Marketing Overview ",
                        null,
                        " role ",
                        " analyst ",
                        " view ",
                        " allow "));
        service.upsertResourcePermission(7L, "operator", new BiResourcePermissionCommand(
                "chart",
                "Orders Trend",
                22L,
                "USER",
                "alice",
                "export",
                "deny"));
        service.upsertResourcePermission(42L, "operator", new BiResourcePermissionCommand(
                "dashboard",
                "Marketing Overview",
                null,
                "ALL",
                "*",
                "view",
                "allow"));

        assertThat(resource.tenantId()).isEqualTo(7L);
        assertThat(resource.resourceType()).isEqualTo("DASHBOARD");
        assertThat(resource.resourceKey()).isEqualTo("marketing-overview");
        assertThat(resource.resourceId()).isEqualTo(resource.resourceKey().hashCode() & 0x7fffffffL);
        assertThat(resource.subjectType()).isEqualTo("ROLE");
        assertThat(resource.subjectId()).isEqualTo("analyst");
        assertThat(resource.actionKey()).isEqualTo("VIEW");
        assertThat(resource.effect()).isEqualTo("ALLOW");
        assertThat(resource.createdBy()).isEqualTo("analyst");

        assertThat(service.listResourcePermissions(7L, null, null, null))
                .extracting(BiResourcePermissionView::resourceType, BiResourcePermissionView::resourceKey)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CHART", "orders-trend"),
                        org.assertj.core.groups.Tuple.tuple("DASHBOARD", "marketing-overview"));
        assertThat(service.listResourcePermissions(7L, " dashboard ", " Marketing Overview ", null))
                .extracting(BiResourcePermissionView::id)
                .containsExactly(resource.id());
        assertThat(service.listResourcePermissions(42L, null, null, null))
                .extracting(BiResourcePermissionView::tenantId)
                .containsExactly(42L);

        assertThat(service.upsertRowPermission(7L, "analyst", new BiRowPermissionCommand(
                " Orders Daily ",
                " CN Only ",
                "ROLE",
                "analyst",
                List.of(Map.of("field", "country", "op", "EQ", "value", "CN")),
                Map.of(),
                null)).filterJson()).contains("country");
        assertThat(service.upsertColumnPermission(7L, "analyst", new BiColumnPermissionCommand(
                " Orders Daily ",
                " Customer Phone ",
                "USER",
                "alice",
                " mask ",
                Map.of("strategy", "last4"),
                null)).maskJson()).contains("last4");

        service.deleteResourcePermission(7L, "analyst", resource.id());
        service.deleteResourcePermission(7L, "analyst", resource.id());
        assertThat(service.listResourcePermissions(7L, "dashboard", "marketing-overview", null)).isEmpty();

        BiPermissionRequestView request = service.requestPermission(7L, " requester ",
                new BiPermissionRequestCommand(
                        " dashboard ",
                        " Marketing Overview ",
                        " export ",
                        "Need campaign export"));
        BiPermissionRequestView reviewed = service.reviewPermissionRequest(7L, " reviewer ",
                new BiPermissionRequestReviewCommand(request.id(), " approved ", "go ahead"));

        assertThat(request.status()).isEqualTo("PENDING");
        assertThat(request.requestedBy()).isEqualTo("requester");
        assertThat(reviewed.status()).isEqualTo("APPROVED");
        assertThat(reviewed.reviewedBy()).isEqualTo("reviewer");
        assertThat(reviewed.grantedPermissionId()).isNotNull();
        assertThat(service.listPermissionRequests(7L, "DASHBOARD", "marketing-overview", "approved"))
                .extracting(BiPermissionRequestView::id)
                .containsExactly(request.id());
        assertThat(service.permissionAudit(7L, 2))
                .hasSize(2)
                .allSatisfy(entry -> assertThat(entry.actorId()).isNotBlank());
    }

    @Test
    void permissionAdministrationGrantsAndDeletesDriveEffectiveAccess() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        BiResourcePermissionView grant = service.upsertResourcePermission(7L, " analyst ",
                new BiResourcePermissionCommand(
                        " dashboard ",
                        " Marketing Overview ",
                        null,
                        " role ",
                        " analyst ",
                        " view ",
                        " allow "));

        assertThat(repository.grants)
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.id()).isNotEqualTo(grant.id());
                    assertThat(saved.workspaceId()).isEqualTo(grant.workspaceId());
                    assertThat(saved.resourceId()).isEqualTo(grant.resourceId());
                    assertThat(saved.subjectType()).isEqualTo("ROLE");
                    assertThat(saved.actionKey()).isEqualTo("VIEW");
                });

        BiPermissionDecisionView allowed = service.effectiveAccess(new BiAccessRequest(
                7L,
                grant.workspaceId(),
                "dashboard",
                grant.resourceId(),
                "alice",
                Set.of("analyst"),
                "view"));

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.effect()).isEqualTo("ALLOW");

        service.deleteResourcePermission(7L, "analyst", grant.id());

        assertThat(repository.grants).isEmpty();
        assertThat(service.effectiveAccess(new BiAccessRequest(
                7L,
                grant.workspaceId(),
                "dashboard",
                grant.resourceId(),
                "alice",
                Set.of("analyst"),
                "view")).allowed()).isFalse();
    }

    @Test
    void subscriptionDeliveryCatalogScopesNormalizesRunsRetriesAttachmentsAndAuditCounts() {
        FakeRepository repository = new FakeRepository();
        BiCatalogApplicationService service = new BiCatalogApplicationService(repository, repository, repository,
                repository, repository, CLOCK);

        var subscription = service.upsertSubscription(7L, new BiSubscriptionCommand(
                " Daily Revenue ",
                "Daily revenue",
                " dashboard ",
                " Marketing Overview ",
                300L,
                Map.of("cron", "0 8 * * *"),
                Map.of("email", List.of("ops@example.com")),
                Map.of("channel", "email"),
                true), " analyst ");
        var alert = service.upsertAlertRule(7L, new BiAlertRuleCommand(
                " GMV Spike ",
                "GMV spike",
                " Orders Daily ",
                " Gross GMV ",
                Map.of("op", "GT", "value", 1000),
                Map.of("email", List.of("growth@example.com")),
                true), "operator");
        service.upsertSubscription(42L, new BiSubscriptionCommand(
                " Daily Revenue ",
                "Other tenant",
                "dashboard",
                "Marketing Overview",
                300L,
                Map.of(),
                Map.of(),
                Map.of(),
                true), "other");

        var subscriptionRun = service.runSubscriptionDelivery(7L, subscription.id(), "scheduler");
        var alertRun = service.runAlertDelivery(7L, alert.id(), "analyst");
        var retry = service.retryDeliveryLogs(7L, "operator", 10);
        var audit = service.auditDeliveryLogs(7L, null, null, null, null, 10);
        var attachment = service.listDeliveryAttachments(7L, " subscription ", subscription.id(), null, 10).get(0);
        BiDeliveryAttachmentDownload download = service.downloadDeliveryAttachment(7L, attachment.id(), " analyst ");

        assertThat(subscription.subscriptionKey()).isEqualTo("daily-revenue");
        assertThat(subscription.resourceType()).isEqualTo("DASHBOARD");
        assertThat(subscription.resourceKey()).isEqualTo("marketing-overview");
        assertThat(alert.alertKey()).isEqualTo("gmv-spike");
        assertThat(alert.datasetKey()).isEqualTo("orders-daily");
        assertThat(alert.metricKey()).isEqualTo("gross-gmv");
        assertThat(service.listSubscriptions(7L, 10)).extracting("id").containsExactly(subscription.id());
        assertThat(service.listSubscriptions(42L, 10)).extracting("name").containsExactly("Other tenant");

        assertThat(subscriptionRun.jobType()).isEqualTo("SUBSCRIPTION");
        assertThat(subscriptionRun.status()).isEqualTo("TRIGGERED");
        assertThat(alertRun.jobType()).isEqualTo("ALERT");
        assertThat(service.listDeliveryLogs(7L, null, null, 10))
                .extracting("jobType")
                .containsExactly("SUBSCRIPTION", "ALERT", "ALERT", "SUBSCRIPTION");
        assertThat(retry.checked()).isEqualTo(2);
        assertThat(retry.retried()).isEqualTo(2);
        assertThat(audit.total()).isEqualTo(4);
        assertThat(audit.triggered()).isEqualTo(2);
        assertThat(audit.delivered()).isEqualTo(2);
        assertThat(download.filename()).contains("daily-revenue");
        assertThat(download.contentType()).isEqualTo("text/plain");
        assertThat(download.bytes()).isNotEmpty();

        service.deleteSubscription(7L, subscription.id());
        service.deleteSubscription(7L, subscription.id());
        assertThat(service.listSubscriptions(7L, 10)).isEmpty();
        assertThat(service.runDeliveryScheduler(7L, "scheduler").subscriptionsChecked()).isZero();
        assertThat(service.cleanupDeliveryAttachments(7L, 10).filesDeleted()).isPositive();
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
            grants.removeIf(existing -> saved.id().equals(existing.id()));
            grants.add(saved);
            return saved;
        }

        @Override
        public void deleteGrant(Long tenantId,
                                Long workspaceId,
                                String resourceType,
                                Long resourceId,
                                String subjectType,
                                String subjectId,
                                String actionKey) {
            grants.removeIf(grant -> tenantId.equals(grant.tenantId())
                    && workspaceId.equals(grant.workspaceId())
                    && resourceType.equals(grant.resourceType())
                    && resourceId.equals(grant.resourceId())
                    && subjectType.equals(grant.subjectType())
                    && subjectId.equals(grant.subjectId())
                    && actionKey.equals(grant.actionKey()));
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
