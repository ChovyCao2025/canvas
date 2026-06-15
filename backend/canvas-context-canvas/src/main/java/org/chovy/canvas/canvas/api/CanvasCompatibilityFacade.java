package org.chovy.canvas.canvas.api;

import java.util.Map;

import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.ApprovalStatusView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.CanvasExportView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.CanvasImportView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.CanvasView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.DiffView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.MessagePreviewView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.OperationView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.PageView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.PrePublishCheckView;
import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService.ReviewView;

public interface CanvasCompatibilityFacade {

    CanvasView create(Long tenantId, String operator, Map<String, ?> request);

    CanvasView get(Long tenantId, Long canvasId);

    CanvasView update(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    PageView<CanvasView> list(Long tenantId);

    ReviewView submitReview(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    ApprovalStatusView approvalStatusView(Long tenantId, Long canvasId);

    PrePublishCheckView prePublishChecksView(Long tenantId, Long canvasId);

    OperationView revert(Long tenantId, String operator, Long canvasId, Long versionId);

    CanvasView startCanary(Long tenantId, String operator, Long canvasId, int percent);

    CanvasView promoteCanary(Long tenantId, String operator, Long canvasId);

    CanvasView rollbackCanary(Long tenantId, String operator, Long canvasId);

    CanvasView rollback(Long tenantId, String operator, Long canvasId);

    CanvasView cloneCanvas(Long tenantId, String operator, Long canvasId);

    DiffView diff(Long tenantId, Long canvasId, Long leftVersionId, Long rightVersionId);

    CanvasView safeUpdate(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    MessagePreviewView previewMessage(Long tenantId, Long canvasId, Map<String, ?> request);

    CanvasExportView exportCanvasView(Long tenantId, Long canvasId, Long versionId);

    CanvasImportView importCanvas(Long tenantId, Map<String, ?> request);
}
