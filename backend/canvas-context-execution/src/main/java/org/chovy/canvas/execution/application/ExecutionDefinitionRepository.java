package org.chovy.canvas.execution.application;

import java.util.Optional;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;

public interface ExecutionDefinitionRepository extends PublishedCanvasDefinitionProvider {

    void save(PublishedCanvasDefinition definition);

    Optional<PublishedCanvasDefinition> findPublished(Long tenantId, Long canvasId);

    void remove(Long tenantId, Long canvasId);

    @Override
    default PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId) {
        return findPublished(tenantId, canvasId)
                .orElseThrow(() -> new IllegalStateException(
                        "published canvas definition not found: tenantId=" + tenantId + ", canvasId=" + canvasId));
    }
}
