package org.chovy.canvas.execution.api.node;

import java.util.List;

/**
 * 定义 NodeMetadataFacade 的执行上下文数据结构或业务契约。
 */
public interface NodeMetadataFacade {

    /**
     * 执行 listNodeTypes 对应的业务处理。
     * @return 处理后的结果
     */
    List<NodeMetadataView> listNodeTypes();

    /**
     * 执行 getNodeTypeSchema 对应的业务处理。
     * @param typeKey typeKey 参数
     */
    NodeMetadataView getNodeTypeSchema(String typeKey);
}
