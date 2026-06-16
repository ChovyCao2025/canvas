package org.chovy.canvas.canvas.api;

/**
 * 定义ExecutionPublicationPort对外提供的能力契约。
 */
public interface ExecutionPublicationPort {

    /**
     * 处理publish。
     */
    void publish(PublishedCanvasDefinition definition);

    /**
     * 处理unpublish。
     */
    void unpublish(Long tenantId, Long canvasId);
}
