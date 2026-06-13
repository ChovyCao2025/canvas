package org.chovy.canvas.canvas.api;

public interface ExecutionPublicationPort {

    void publish(PublishedCanvasDefinition definition);

    void unpublish(Long tenantId, Long canvasId);
}
