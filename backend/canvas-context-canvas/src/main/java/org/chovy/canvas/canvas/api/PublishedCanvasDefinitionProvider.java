package org.chovy.canvas.canvas.api;

public interface PublishedCanvasDefinitionProvider {

    PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId);
}
