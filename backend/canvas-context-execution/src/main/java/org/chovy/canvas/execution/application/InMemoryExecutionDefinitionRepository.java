package org.chovy.canvas.execution.application;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.springframework.stereotype.Repository;

/**
 * 定义 InMemoryExecutionDefinitionRepository 的执行上下文数据结构或业务契约。
 */
@Repository
public class InMemoryExecutionDefinitionRepository implements ExecutionDefinitionRepository {

    private final Map<Key, PublishedCanvasDefinition> definitions = new ConcurrentHashMap<>();

    /**
     * 执行 save 对应的业务处理。
     * @param definition definition 参数
     */
    @Override
    public void save(PublishedCanvasDefinition definition) {
        Objects.requireNonNull(definition, "definition is required");
        definitions.put(new Key(definition.tenantId(), definition.canvasId()), definition);
    }

    /**
     * 执行 findPublished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @return 处理后的结果
     */
    @Override
    public Optional<PublishedCanvasDefinition> findPublished(Long tenantId, Long canvasId) {
        return Optional.ofNullable(definitions.get(new Key(tenantId, canvasId)));
    }

    /**
     * 执行 remove 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     */
    @Override
    public void remove(Long tenantId, Long canvasId) {
        definitions.remove(new Key(tenantId, canvasId));
    }

    /**
     * 定义 Key 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     */
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
