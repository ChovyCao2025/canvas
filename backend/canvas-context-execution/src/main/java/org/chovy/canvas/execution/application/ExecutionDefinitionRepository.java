package org.chovy.canvas.execution.application;

import java.util.Optional;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;

/**
 * 定义 ExecutionDefinitionRepository 的执行上下文数据结构或业务契约。
 */
public interface ExecutionDefinitionRepository extends PublishedCanvasDefinitionProvider {

    /**
     * 执行 save 对应的业务处理。
     * @param definition definition 参数
     */
    void save(PublishedCanvasDefinition definition);

    /**
     * 执行 findPublished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @return 处理后的结果
     */
    Optional<PublishedCanvasDefinition> findPublished(Long tenantId, Long canvasId);

    /**
     * 执行 remove 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    void remove(Long tenantId, Long canvasId);

    /**
     * 执行 getPublished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    @Override
    default PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId) {
        return findPublished(tenantId, canvasId)
                .orElseThrow(() -> new IllegalStateException(
                        "published canvas definition not found: tenantId=" + tenantId + ", canvasId=" + canvasId));
    }
}
