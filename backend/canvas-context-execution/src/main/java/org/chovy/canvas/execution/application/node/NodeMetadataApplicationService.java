package org.chovy.canvas.execution.application.node;

import java.util.Comparator;
import java.util.List;

import org.chovy.canvas.execution.api.node.NodeMetadataFacade;
import org.chovy.canvas.execution.api.node.NodeMetadataView;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.chovy.canvas.execution.domain.NodeMetadata;
import org.springframework.stereotype.Service;

/**
 * 定义 NodeMetadataApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class NodeMetadataApplicationService implements NodeMetadataFacade {

    /**
     * 保存 registry 对应的状态或配置。
     */
    private final NodeHandlerRegistry registry;

    /**
     * 执行 NodeMetadataApplicationService 对应的业务处理。
     * @param registry registry 参数
     */
    public NodeMetadataApplicationService(NodeHandlerRegistry registry) {
        this.registry = registry;
    }

    /**
     * 执行 listNodeTypes 对应的业务处理。
     * @return 处理后的结果
     */
    @Override
    public List<NodeMetadataView> listNodeTypes() {
        return registry.metadata().stream()
                .map(NodeMetadataApplicationService::toView)
                .sorted(Comparator.comparing(NodeMetadataView::nodeType))
                .toList();
    }

    /**
     * 执行 getNodeTypeSchema 对应的业务处理。
     * @param typeKey typeKey 参数
     */
    @Override
    public NodeMetadataView getNodeTypeSchema(String typeKey) {
        if (typeKey == null || typeKey.isBlank()) {
            throw new IllegalArgumentException("node type is required");
        }
        return registry.metadata().stream()
                .filter(metadata -> metadata.nodeType().equals(typeKey))
                .findFirst()
                .map(NodeMetadataApplicationService::toView)
                .orElseThrow(() -> new IllegalArgumentException("unknown node type: " + typeKey));
    }

    /**
     * 执行 toView 对应的业务处理。
     * @param metadata metadata 参数
     */
    private static NodeMetadataView toView(NodeMetadata metadata) {
        return new NodeMetadataView(
                metadata.nodeType(),
                metadata.displayName(),
                metadata.category(),
                metadata.configSchemaJson(),
                metadata.inputPorts(),
                metadata.outputPorts(),
                metadata.requiredPluginId(),
                metadata.enabled(),
                metadata.disabledReason());
    }
}
