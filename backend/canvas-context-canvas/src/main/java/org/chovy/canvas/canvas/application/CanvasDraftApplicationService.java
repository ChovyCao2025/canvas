package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStateTransitionPolicy;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasDraftApplicationService {

    private final CanvasRepository canvasRepository;
    private final CanvasVersionRepository versionRepository;
    private final CanvasStateTransitionPolicy transitionPolicy;

    public CanvasDraftApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository) {
        this(canvasRepository, versionRepository, new CanvasStateTransitionPolicy());
    }

    public CanvasDraftApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository,
                                         CanvasStateTransitionPolicy transitionPolicy) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
        this.transitionPolicy = transitionPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public Canvas createDraft(CreateDraftCommand command) {
        Canvas canvas = canvasRepository.save(Canvas.createDraft(
                null,
                command.tenantId(),
                command.name(),
                command.description(),
                command.operator()));
        if (command.graphJson() != null && !command.graphJson().isBlank()) {
            versionRepository.save(CanvasVersion.draft(
                    null,
                    canvas.id(),
                    canvas.tenantId(),
                    1,
                    command.graphJson(),
                    command.operator()));
        }
        return canvas;
    }

    @Transactional(rollbackFor = Exception.class)
    public Canvas updateDraft(Long canvasId, UpdateDraftCommand command) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertDraftUpdateAllowed(canvas);
        Canvas updated = canvasRepository.save(canvas.updateMetadata(command.name(), command.description()));

        if (command.graphJson() == null) {
            return updated;
        }
        if (transitionPolicy.isPublished(canvas)) {
            versionRepository.save(CanvasVersion.draft(
                    null,
                    canvas.id(),
                    canvas.tenantId(),
                    versionRepository.nextVersion(canvas.id()),
                    command.graphJson(),
                    command.operator()));
            return updated;
        }
        versionRepository.latestDraft(canvas.id())
                .map(existing -> versionRepository.save(existing.withGraphJson(command.graphJson())))
                .orElseGet(() -> versionRepository.save(CanvasVersion.draft(
                        null,
                        canvas.id(),
                        canvas.tenantId(),
                        versionRepository.nextVersion(canvas.id()),
                        command.graphJson(),
                        command.operator())));
        return updated;
    }

    private Canvas requireCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }

    public record CreateDraftCommand(
            Long tenantId,
            String name,
            String description,
            String graphJson,
            String operator) {
    }

    public record UpdateDraftCommand(
            String name,
            String description,
            String graphJson,
            String operator) {
    }
}
