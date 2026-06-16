package org.chovy.canvas.execution.domain;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;

/**
 * 定义 DagParser 的执行上下文数据结构或业务契约。
 */
public class DagParser {

    /**
     * 保存 runtimeService 对应的状态或配置。
     */
    private final DagRuntimeService runtimeService;

    /**
     * 执行 DagParser 对应的业务处理。
     */
    public DagParser() {
        this(new DagRuntimeService());
    }

    /**
     * 执行 DagParser 对应的业务处理。
     * @param runtimeService runtimeService 参数
     */
    public DagParser(DagRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /**
     * 执行 parse 对应的业务处理。
     * @param definition definition 参数
     * @return 处理后的结果
     */
    public DagGraph parse(PublishedCanvasDefinition definition) {
        return runtimeService.validate(definition);
    }
}
