package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 封装CanvasCompatibilityApplicationServiceTest相关的业务逻辑。
 */
class CanvasCompatibilityApplicationServiceTest {

    /**
     * 创建GetUpdateAndListAreTenantScopedWithDeterministicOrdering。
     */
    @Test
    void createGetUpdateAndListAreTenantScopedWithDeterministicOrdering() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();

        CanvasCompatibilityApplicationService.CanvasView first = service.create(7L, "alice", Map.of(
                "name", "Welcome Flow",
                "description", "first",
                "graphJson", "{\"nodes\":[\"start\"]}"));
        CanvasCompatibilityApplicationService.CanvasView second = service.create(7L, "bob", Map.of(
                "name", "Retention Flow",
                "description", "second",
                "graphJson", "{\"nodes\":[\"retention\"]}"));
        service.create(8L, "mallory", Map.of("name", "Other Tenant"));

        assertThat(first)
                .returns(1000L, CanvasCompatibilityApplicationService.CanvasView::id)
                .returns(7L, CanvasCompatibilityApplicationService.CanvasView::tenantId)
                .returns("Welcome Flow", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns("first", CanvasCompatibilityApplicationService.CanvasView::description)
                .returns("DRAFT", CanvasCompatibilityApplicationService.CanvasView::status)
                .returns(1, CanvasCompatibilityApplicationService.CanvasView::editVersion)
                .returns("alice", CanvasCompatibilityApplicationService.CanvasView::createdBy);

        CanvasCompatibilityApplicationService.CanvasView updated = service.update(7L, "carol", 1000L, Map.of(
                "name", "Welcome Flow v2",
                "description", "updated",
                "graphJson", "{\"nodes\":[\"updated\"]}"));
        assertThat(updated)
                .returns(1000L, CanvasCompatibilityApplicationService.CanvasView::id)
                .returns("Welcome Flow v2", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns(2, CanvasCompatibilityApplicationService.CanvasView::editVersion)
                .returns("carol", CanvasCompatibilityApplicationService.CanvasView::updatedBy);

        assertThat(service.get(7L, 1000L).graphJson()).isEqualTo("{\"nodes\":[\"updated\"]}");
        assertThat(service.list(7L).list())
                .extracting(CanvasCompatibilityApplicationService.CanvasView::id)
                .containsExactly(first.id(), second.id());
        assertThat(service.list(8L).list())
                .extracting(CanvasCompatibilityApplicationService.CanvasView::tenantId)
                .containsExactly(8L);
    }

    /**
     * 处理workflowOperationsModelReviewPrepublishCloneRevertCanaryAndImportExport。
     */
    @Test
    void workflowOperationsModelReviewPrepublishCloneRevertCanaryAndImportExport() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(7L, "operator-1", Map.of(
                "name", "Launch Flow",
                "graphJson", "{\"nodes\":[\"draft\"]}"));
        service.update(7L, "operator-1", 1000L, Map.of("graphJson", "{\"nodes\":[\"v2\"]}"));

        assertThat(service.submitReview(7L, "reviewer", 1000L, Map.of("reason", "ready")))
                .returns("SUBMITTED", CanvasCompatibilityApplicationService.ReviewView::status)
                .returns("reviewer", CanvasCompatibilityApplicationService.ReviewView::operator);
        assertThat(service.approvalStatusView(7L, 1000L))
                .returns("SUBMITTED", CanvasCompatibilityApplicationService.ApprovalStatusView::status)
                .returns(1000L, CanvasCompatibilityApplicationService.ApprovalStatusView::canvasId);
        assertThat(service.prePublishChecksView(7L, 1000L))
                .returns(true, CanvasCompatibilityApplicationService.PrePublishCheckView::passed);

        assertThat(service.revert(7L, "operator-1", 1000L, 1L))
                .returns(1000L, CanvasCompatibilityApplicationService.OperationView::canvasId)
                .returns(1L, CanvasCompatibilityApplicationService.OperationView::versionId);
        assertThat(service.startCanary(7L, "operator-1", 1000L, 25))
                .returns(25, CanvasCompatibilityApplicationService.CanvasView::canaryPercent);
        assertThat(service.promoteCanary(7L, "operator-1", 1000L))
                .returns("PUBLISHED", CanvasCompatibilityApplicationService.CanvasView::status);
        assertThat(service.rollbackCanary(7L, "operator-1", 1000L))
                .returns(0, CanvasCompatibilityApplicationService.CanvasView::canaryPercent);
        assertThat(service.rollback(7L, "operator-1", 1000L))
                .returns("ROLLED_BACK", CanvasCompatibilityApplicationService.CanvasView::status);

        CanvasCompatibilityApplicationService.CanvasView clone = service.cloneCanvas(7L, "operator-1", 1000L);
        assertThat(clone)
                .returns(1001L, CanvasCompatibilityApplicationService.CanvasView::id)
                .returns("Launch Flow copy", CanvasCompatibilityApplicationService.CanvasView::name);

        assertThat(service.diff(7L, 1000L, 1L, 2L))
                .returns(true, CanvasCompatibilityApplicationService.DiffView::changed);
        assertThat(service.previewMessage(7L, 1000L, Map.of("nodeId", "send-1", "userId", "u-1")))
                .returns(1000L, CanvasCompatibilityApplicationService.MessagePreviewView::canvasId)
                .returns("send-1", CanvasCompatibilityApplicationService.MessagePreviewView::nodeId);
        assertThat(service.exportCanvasView(7L, 1000L, 1L))
                .returns(1000L, CanvasCompatibilityApplicationService.CanvasExportView::canvasId)
                .returns(1L, CanvasCompatibilityApplicationService.CanvasExportView::versionId);
        assertThat(service.importCanvas(7L, Map.of(
                "name", "Imported Flow",
                "operator", "importer",
                "graphJson", "{\"nodes\":[\"imported\"]}")).created())
                .returns(1002L, CanvasCompatibilityApplicationService.CanvasView::id)
                .returns("Imported Flow", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns("importer", CanvasCompatibilityApplicationService.CanvasView::createdBy);
    }

    /**
     * 处理safeUpdateRejectsStaleEditVersion。
     */
    @Test
    void safeUpdateRejectsStaleEditVersion() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(7L, "operator-1", Map.of("name", "Safe Flow"));
        service.update(7L, "operator-1", 1000L, Map.of("name", "Safe Flow v2"));

        assertThatThrownBy(() -> service.safeUpdate(7L, "operator-1", 1000L, Map.of(
                "name", "stale",
                "editVersion", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CANVAS_010");

        assertThat(service.safeUpdate(7L, "operator-1", 1000L, Map.of(
                "name", "safe",
                "editVersion", 2)))
                .returns("safe", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns(3, CanvasCompatibilityApplicationService.CanvasView::editVersion);
    }

    /**
     * 处理templateRoutesListSaveCreateCanvasAndExposePendingReviews。
     */
    @Test
    void templateRoutesListSaveCreateCanvasAndExposePendingReviews() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(7L, "operator-1", Map.of(
                "name", "Lifecycle Flow",
                "description", "welcome lifecycle",
                "graphJson", "{\"nodes\":[\"start\"]}"));
        service.update(7L, "operator-1", 1000L, Map.of("graphJson", "{\"nodes\":[\"draft\"]}"));
        service.submitReview(7L, "reviewer", 1000L, Map.of("reason", "launch"));

        CanvasCompatibilityApplicationService.TemplateView saved = service.saveAsTemplate(7L, 1000L, Map.of(
                "name", "Lifecycle Template",
                "description", "Reusable lifecycle",
                "category", "lifecycle"));
        assertThat(saved)
                .returns(5000L, CanvasCompatibilityApplicationService.TemplateView::id)
                .returns("Lifecycle Template", CanvasCompatibilityApplicationService.TemplateView::name)
                .returns("lifecycle", CanvasCompatibilityApplicationService.TemplateView::category)
                .returns("{\"nodes\":[\"draft\"]}", CanvasCompatibilityApplicationService.TemplateView::graphJson)
                .returns(0, CanvasCompatibilityApplicationService.TemplateView::useCount);

        assertThat(service.listTemplates(7L, "lifecycle"))
                .extracting(CanvasCompatibilityApplicationService.TemplateView::id)
                .containsExactly(5000L);

        CanvasCompatibilityApplicationService.CanvasView created = service.createFromTemplate(7L, 5000L,
                Map.of("name", "Copied Lifecycle"), "creator");
        assertThat(created)
                .returns(1001L, CanvasCompatibilityApplicationService.CanvasView::id)
                .returns("Copied Lifecycle", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns("{\"nodes\":[\"draft\"]}", CanvasCompatibilityApplicationService.CanvasView::graphJson)
                .returns("creator", CanvasCompatibilityApplicationService.CanvasView::createdBy);
        assertThat(service.listTemplates(7L, null).getFirst().useCount()).isEqualTo(1);

        assertThat(service.pendingReviews(7L))
                .extracting(CanvasCompatibilityApplicationService.PendingReviewView::reviewId)
                .containsExactly("review-1000");
    }

    /**
     * 处理batchOperationsNormalizeOperationAndReturnPerCanvasStatuses。
     */
    @Test
    void batchOperationsNormalizeOperationAndReturnPerCanvasStatuses() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(7L, "operator-1", Map.of("name", "Active Flow"));
        service.create(7L, "operator-1", Map.of("name", "Clonable Flow"));

        CanvasCompatibilityApplicationService.BatchOperationView archived = service.batchOperation(7L, "admin",
                "archive", Map.of(
                        "canvasIds", java.util.List.of(1000L, 404L),
                        "filter", Map.of("status", "DRAFT"),
                        "replacements", Map.of("reason", "cleanup")));

        assertThat(archived)
                .returns("ARCHIVE", CanvasCompatibilityApplicationService.BatchOperationView::operation)
                .returns(2, CanvasCompatibilityApplicationService.BatchOperationView::totalCount)
                .returns(1, CanvasCompatibilityApplicationService.BatchOperationView::successCount)
                .returns(0, CanvasCompatibilityApplicationService.BatchOperationView::skippedCount)
                .returns(1, CanvasCompatibilityApplicationService.BatchOperationView::failedCount);
        assertThat(archived.items())
                .extracting(
                        CanvasCompatibilityApplicationService.BatchItemView::canvasId,
                        CanvasCompatibilityApplicationService.BatchItemView::status,
                        CanvasCompatibilityApplicationService.BatchItemView::message)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1000L, "SUCCESS", "ARCHIVED"),
                        org.assertj.core.groups.Tuple.tuple(404L, "FAILED", "画布不存在: 404"));
        assertThat(service.get(7L, 1000L).status()).isEqualTo("ARCHIVED");

        CanvasCompatibilityApplicationService.BatchOperationView cloned = service.batchOperation(7L, "admin",
                "ClOnE", Map.of("canvasIds", java.util.List.of(1001L)));

        assertThat(cloned.operation()).isEqualTo("CLONE");
        assertThat(cloned.items())
                .extracting(
                        CanvasCompatibilityApplicationService.BatchItemView::canvasId,
                        CanvasCompatibilityApplicationService.BatchItemView::status,
                        CanvasCompatibilityApplicationService.BatchItemView::targetCanvasId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1001L, "SUCCESS", 1002L));
        assertThat(service.get(7L, 1002L))
                .returns("Clonable Flow copy", CanvasCompatibilityApplicationService.CanvasView::name)
                .returns(1001L, CanvasCompatibilityApplicationService.CanvasView::sourceCanvasId);
    }

}
