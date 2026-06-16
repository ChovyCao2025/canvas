package org.chovy.canvas.canvas.application;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;
import org.chovy.canvas.canvas.domain.Canvas;
import org.chovy.canvas.canvas.domain.CanvasListItem;
import org.chovy.canvas.canvas.domain.CanvasListQuery;
import org.chovy.canvas.canvas.domain.CanvasPage;
import org.chovy.canvas.canvas.domain.CanvasRepository;
import org.chovy.canvas.canvas.domain.CanvasStatus;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.chovy.canvas.canvas.domain.CanvasVersionRepository;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasQueryApplicationService相关的业务逻辑。
 */
@Service
public class CanvasQueryApplicationService implements PublishedCanvasDefinitionProvider {

    /**
     * 保存canvasRepository。
     */
    private final CanvasRepository canvasRepository;

    /**
     * 保存versionRepository。
     */
    private final CanvasVersionRepository versionRepository;

    /**
     * 使用画布仓储创建画布查询应用服务。
     */
    public CanvasQueryApplicationService(CanvasRepository canvasRepository,
                                         CanvasVersionRepository versionRepository) {
        this.canvasRepository = canvasRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * 获取Canvas。
     */
    public Canvas getCanvas(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("画布不存在: " + canvasId));
    }

    /**
     * 分页查询画布列表。
     */
    public CanvasPage<CanvasListItem> listCanvases(CanvasListQuery query) {
        return canvasRepository.list(query);
    }

    /**
     * 获取Published。
     */
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
