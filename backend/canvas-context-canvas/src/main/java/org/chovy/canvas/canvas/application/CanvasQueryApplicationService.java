package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;

@Service
public class CanvasQueryApplicationService implements PublishedCanvasDefinitionProvider {

    private final CanvasRepository canvasRepository;
    private final CanvasVersionRepository versionRepository;

    public CanvasQueryApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
    }

    public Canvas getCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }

    @Override
    public PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId) {
        Canvas canvas = getCanvas(canvasId);
        if (!canvas.tenantId().equals(tenantId)) {
            throw new IllegalArgumentException("画布不存在: " + canvasId);
        }
        if (canvas.status() != CanvasStatus.PUBLISHED || canvas.publishedVersionId() == null) {
            throw new IllegalStateException("画布未发布: " + canvasId);
        }
        CanvasVersion version = versionRepository.findById(canvas.publishedVersionId())
                .orElseThrow(() -> new IllegalStateException("发布版本不存在: " + canvas.publishedVersionId()));
        return PublishedCanvasDefinitionAssembler.assemble(canvas, version, java.time.Instant.now());
    }
}
