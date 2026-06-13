package org.chovy.canvas.execution.api.node;

import java.util.List;

public interface NodeMetadataFacade {

    List<NodeMetadataView> listNodeTypes();

    NodeMetadataView getNodeTypeSchema(String typeKey);
}
