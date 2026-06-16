package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;
import org.chovy.canvas.execution.domain.ExecutionRequestCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 ExecutionRequestApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionRequestApplicationService implements ExecutionRequestFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final ExecutionRequestCatalog catalog;

    /**
     * 执行 ExecutionRequestApplicationService 对应的业务处理。
     */
    public ExecutionRequestApplicationService() {
        this(new ExecutionRequestCatalog());
    }

    /**
     * 执行 ExecutionRequestApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public ExecutionRequestApplicationService(ExecutionRequestCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public RequestPageView list(RequestQuery query) {
        return catalog.list(query);
    }

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param command command 参数
     * @return 处理后的结果
     */
    @Override
    public ReplayResult replay(String id, ReplayCommand command) {
        return catalog.replay(id, command);
    }

    /**
     * 执行 replayBatch 对应的业务处理。
     * @param command command 参数
     */
    @Override
    public BatchReplayResult replayBatch(BatchReplayCommand command) {
        return catalog.replayBatch(command);
    }

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    @Override
    public void register(RequestCommand command) {
        catalog.register(command);
    }
}
