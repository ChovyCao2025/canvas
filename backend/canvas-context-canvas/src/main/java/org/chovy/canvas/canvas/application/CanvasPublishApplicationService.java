package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStateTransitionPolicy;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasPublishApplicationService {

    private final CanvasRepository canvasRepository;
    private final CanvasVersionRepository versionRepository;
    private final ExecutionPublicationPort publicationPort;
    private final CanvasStateTransitionPolicy transitionPolicy;

    public CanvasPublishApplicationService(CanvasRepository canvasRepository,
                                           CanvasVersionRepository versionRepository,
                                           ExecutionPublicationPort publicationPort) {
        this(canvasRepository, versionRepository, publicationPort, new CanvasStateTransitionPolicy());
    }

    public CanvasPublishApplicationService(CanvasRepository canvasRepository,
                                           CanvasVersionRepository versionRepository,
                                           ExecutionPublicationPort publicationPort,
                                           CanvasStateTransitionPolicy transitionPolicy) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
        this.publicationPort = publicationPort;
        this.transitionPolicy = transitionPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public CanvasVersion publish(Long canvasId, String operator) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.PUBLISHED);
        CanvasVersion draft = versionRepository.latestDraft(canvasId)
                .orElseThrow(() -> new IllegalStateException("没有可发布的草稿"));
        CanvasVersion published = versionRepository.save(CanvasVersion.published(
                null,
                canvas.id(),
                canvas.tenantId(),
                versionRepository.nextVersion(canvas.id()),
                draft.graphJson(),
                operator));
        Canvas publishedCanvas = canvasRepository.save(canvas.publish(published.id()));
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.publish(
                PublishedCanvasDefinitionAssembler.assemble(publishedCanvas, published, java.time.Instant.now())));
        return published;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.OFFLINE);
        Canvas offline = canvasRepository.save(canvas.offline());
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(offline.tenantId(), offline.id()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void kill(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.KILLED);
        Canvas killed = canvasRepository.save(canvas.kill());
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(killed.tenantId(), killed.id()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.ARCHIVED);
        Canvas archived = canvasRepository.save(canvas.archive());
        if (canvas.status() == CanvasStatus.PUBLISHED) {
            AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(archived.tenantId(), archived.id()));
        }
    }

    private Canvas requireCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }
}
