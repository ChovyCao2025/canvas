package org.chovy.canvas.canvas.application;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasCollaborationFacade;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasCollaborationApplicationService相关的业务逻辑。
 */
@Service
public class CanvasCollaborationApplicationService implements CanvasCollaborationFacade {

    /**
     * 保存仓储。
     */
    private final SummaryRepository repository;

    /**
     * 创建当前对象实例。
     */
    public CanvasCollaborationApplicationService() {
        this((tenantId, canvasId) -> null);
    }

    /**
     * 创建当前对象实例。
     */
    CanvasCollaborationApplicationService(SummaryRepository repository) {
        this.repository = repository;
    }

    /**
     * 处理summary。
     */
    @Override
    public Summary summary(Long tenantId, Long canvasId) {
        if (canvasId == null) {
            throw new IllegalArgumentException("canvasId is required");
        }
        Summary summary = repository.summary(safeTenantId(tenantId), canvasId);
        return summary == null ? defaultSummary(canvasId) : summary;
    }

    /**
     * 处理defaultSummary。
     */
    private static Summary defaultSummary(Long canvasId) {
        return new Summary(canvasId, List.of(), 0, 0, 0);
    }

    /**
     * 处理safe tenant标识。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 定义SummaryRepository对外提供的能力契约。
     */
    interface SummaryRepository {

        /**
         * 处理summary。
         */
        Summary summary(Long tenantId, Long canvasId);
    }
}
