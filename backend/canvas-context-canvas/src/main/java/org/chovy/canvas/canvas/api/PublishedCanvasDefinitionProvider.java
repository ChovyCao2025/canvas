package org.chovy.canvas.canvas.api;

/**
 * 定义PublishedCanvasDefinitionProvider对外提供的能力契约。
 */
public interface PublishedCanvasDefinitionProvider {

    /**
     * 获取Published。
     */
    PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId);
}
