package org.chovy.canvas.execution.application;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryExecutionDefinitionRepository implements ExecutionDefinitionRepository {

    private final Map<Key, PublishedCanvasDefinition> definitions = new ConcurrentHashMap<>();

    @Override
    public void save(PublishedCanvasDefinition definition) {
        Objects.requireNonNull(definition, "definition is required");
        definitions.put(new Key(definition.tenantId(), definition.canvasId()), definition);
    }

    @Override
    public Optional<PublishedCanvasDefinition> findPublished(Long tenantId, Long canvasId) {
        return Optional.ofNullable(definitions.get(new Key(tenantId, canvasId)));
    }

    @Override
    public void remove(Long tenantId, Long canvasId) {
        definitions.remove(new Key(tenantId, canvasId));
    }

    private record Key(Long tenantId, Long canvasId) {
        private Key {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (canvasId == null || canvasId <= 0) {
                throw new IllegalArgumentException("canvasId is required");
            }
        }
    }
}
