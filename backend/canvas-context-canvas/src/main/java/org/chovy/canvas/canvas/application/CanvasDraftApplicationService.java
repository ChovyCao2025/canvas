package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStateTransitionPolicy;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 封装CanvasDraftApplicationService相关的业务逻辑。
 */
@Service
public class CanvasDraftApplicationService {

    /**
     * 保存canvasRepository。
     */
    private final CanvasRepository canvasRepository;

    /**
     * 保存versionRepository。
     */
    private final CanvasVersionRepository versionRepository;

    /**
     * 保存transitionPolicy。
     */
    private final CanvasStateTransitionPolicy transitionPolicy;

    /**
     * 使用画布仓储和版本仓储创建草稿应用服务。
     */
    public CanvasDraftApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository) {
        this(canvasRepository, versionRepository, new CanvasStateTransitionPolicy());
    }

    /**
     * 使用画布仓储和版本仓储创建草稿应用服务。
     */
    @Autowired
    public CanvasDraftApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository,
                                         CanvasStateTransitionPolicy transitionPolicy) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
        this.transitionPolicy = transitionPolicy;
    }

    /**
     * 创建Draft。
     */
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

    /**
     * 更新Draft。
     */
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

    /**
     * 校验并返回Canvas。
     */
    private Canvas requireCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }

    /**
     * 承载CreateDraftCommand的数据快照。
     */
    public record CreateDraftCommand(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录graphJSON 内容。
             */
            String graphJson,
            /**
             * 记录操作人。
             */
            String operator) {
    }

    /**
     * 承载UpdateDraftCommand的数据快照。
     */
    public record UpdateDraftCommand(
            /**
             * 记录名称。
             */
            String name,
            /**
             * 记录描述。
             */
            String description,
            /**
             * 记录graphJSON 内容。
             */
            String graphJson,
            /**
             * 记录操作人。
             */
            String operator) {
    }
}
