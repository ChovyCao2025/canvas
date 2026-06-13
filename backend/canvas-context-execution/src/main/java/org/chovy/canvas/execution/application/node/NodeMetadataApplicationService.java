package org.chovy.canvas.execution.application.node;

import java.util.Comparator;
import java.util.List;

import org.chovy.canvas.execution.api.node.NodeMetadataFacade;
import org.chovy.canvas.execution.api.node.NodeMetadataView;
import org.chovy.canvas.execution.domain.NodeHandlerRegistry;
import org.chovy.canvas.execution.domain.NodeMetadata;
import org.springframework.stereotype.Service;

@Service
public class NodeMetadataApplicationService implements NodeMetadataFacade {

    private final NodeHandlerRegistry registry;

    public NodeMetadataApplicationService(NodeHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<NodeMetadataView> listNodeTypes() {
        return registry.metadata().stream()
                .map(NodeMetadataApplicationService::toView)
                .sorted(Comparator.comparing(NodeMetadataView::nodeType))
                .toList();
    }

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
