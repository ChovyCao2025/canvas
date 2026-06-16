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

/**
 * 定义CanvasCompatibilityFacade对外提供的能力契约。
 */
public interface CanvasCompatibilityFacade {

    /**
     * 创建。
     */
    CanvasView create(Long tenantId, String operator, Map<String, ?> request);

    /**
     * 获取。
     */
    CanvasView get(Long tenantId, Long canvasId);

    /**
     * 更新。
     */
    CanvasView update(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    /**
     * 列出。
     */
    PageView<CanvasView> list(Long tenantId);

    /**
     * 处理submitReview。
     */
    ReviewView submitReview(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    /**
     * 处理approvalStatusView。
     */
    ApprovalStatusView approvalStatusView(Long tenantId, Long canvasId);

    /**
     * 处理prePublishChecksView。
     */
    PrePublishCheckView prePublishChecksView(Long tenantId, Long canvasId);

    /**
     * 处理revert。
     */
    OperationView revert(Long tenantId, String operator, Long canvasId, Long versionId);

    /**
     * 处理startCanary。
     */
    CanvasView startCanary(Long tenantId, String operator, Long canvasId, int percent);

    /**
     * 处理promoteCanary。
     */
    CanvasView promoteCanary(Long tenantId, String operator, Long canvasId);

    /**
     * 处理rollbackCanary。
     */
    CanvasView rollbackCanary(Long tenantId, String operator, Long canvasId);

    /**
     * 处理rollback。
     */
    CanvasView rollback(Long tenantId, String operator, Long canvasId);

    /**
     * 处理cloneCanvas。
     */
    CanvasView cloneCanvas(Long tenantId, String operator, Long canvasId);

    /**
     * 处理diff。
     */
    DiffView diff(Long tenantId, Long canvasId, Long leftVersionId, Long rightVersionId);

    /**
     * 处理safeUpdate。
     */
    CanvasView safeUpdate(Long tenantId, String operator, Long canvasId, Map<String, ?> request);

    /**
     * 处理previewMessage。
     */
    MessagePreviewView previewMessage(Long tenantId, Long canvasId, Map<String, ?> request);

    /**
     * 处理exportCanvasView。
     */
    CanvasExportView exportCanvasView(Long tenantId, Long canvasId, Long versionId);

    /**
     * 处理importCanvas。
     */
    CanvasImportView importCanvas(Long tenantId, Map<String, ?> request);
}
