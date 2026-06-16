package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStateTransitionPolicy;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 封装CanvasPublishApplicationService相关的业务逻辑。
 */
@Service
public class CanvasPublishApplicationService {

    /**
     * 保存canvasRepository。
     */
    private final CanvasRepository canvasRepository;

    /**
     * 保存versionRepository。
     */
    private final CanvasVersionRepository versionRepository;

    /**
     * 保存publicationPort。
     */
    private final ExecutionPublicationPort publicationPort;

    /**
     * 保存transitionPolicy。
     */
    private final CanvasStateTransitionPolicy transitionPolicy;

    /**
     * 使用发布所需依赖创建画布发布应用服务。
     */
    @Autowired
    public CanvasPublishApplicationService(CanvasRepository canvasRepository,
                                           CanvasVersionRepository versionRepository,
                                           ExecutionPublicationPort publicationPort) {
        this(canvasRepository, versionRepository, publicationPort, new CanvasStateTransitionPolicy());
    }

    /**
     * 使用发布所需依赖创建画布发布应用服务。
     */
    public CanvasPublishApplicationService(CanvasRepository canvasRepository,
                                           CanvasVersionRepository versionRepository,
                                           ExecutionPublicationPort publicationPort,
                                           CanvasStateTransitionPolicy transitionPolicy) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
        this.publicationPort = publicationPort;
        this.transitionPolicy = transitionPolicy;
    }

    /**
     * 处理publish。
     */
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
        // 事务提交后再通知执行上下文，避免发布失败时外部执行侧看到未提交版本。
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.publish(
                PublishedCanvasDefinitionAssembler.assemble(publishedCanvas, published, java.time.Instant.now())));
        return published;
    }

    /**
     * 处理unpublish。
     */
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.OFFLINE);
        Canvas offline = canvasRepository.save(canvas.offline());
        // 下线消息同样延后到提交后发送，保持持久化状态与运行时发布表一致。
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(offline.tenantId(), offline.id()));
    }

    /**
     * 处理kill。
     */
    @Transactional(rollbackFor = Exception.class)
    public void kill(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.KILLED);
        Canvas killed = canvasRepository.save(canvas.kill());
        // 终止画布会清理执行侧发布定义，必须等待状态写入成功后再广播。
        AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(killed.tenantId(), killed.id()));
    }

    /**
     * 处理archive。
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long canvasId) {
        Canvas canvas = requireCanvas(canvasId);
        transitionPolicy.assertTransition(canvas, CanvasStatus.ARCHIVED);
        Canvas archived = canvasRepository.save(canvas.archive());
        if (canvas.status() == CanvasStatus.PUBLISHED) {
            // 只有已发布画布归档时需要同步撤销执行侧定义。
            AfterCommitExecutor.runAfterCommitOrNow(() -> publicationPort.unpublish(archived.tenantId(), archived.id()));
        }
    }

    /**
     * 校验并返回Canvas。
     */
    private Canvas requireCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }
}
