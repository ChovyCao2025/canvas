package org.chovy.canvas.canvas.application;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasCollaborationFacade;
import org.springframework.stereotype.Service;

@Service
public class CanvasCollaborationApplicationService implements CanvasCollaborationFacade {

    private final SummaryRepository repository;

    public CanvasCollaborationApplicationService() {
        this((tenantId, canvasId) -> null);
    }

    CanvasCollaborationApplicationService(SummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Summary summary(Long tenantId, Long canvasId) {
        if (canvasId == null) {
            throw new IllegalArgumentException("canvasId is required");
        }
        Summary summary = repository.summary(safeTenantId(tenantId), canvasId);
        return summary == null ? defaultSummary(canvasId) : summary;
    }

    private static Summary defaultSummary(Long canvasId) {
        return new Summary(canvasId, List.of(), 0, 0, 0);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    interface SummaryRepository {
        Summary summary(Long tenantId, Long canvasId);
    }
}
