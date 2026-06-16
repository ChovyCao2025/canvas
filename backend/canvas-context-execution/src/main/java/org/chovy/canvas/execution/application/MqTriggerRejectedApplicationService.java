package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.MqTriggerRejectedFacade;
import org.chovy.canvas.execution.domain.MqTriggerRejectedCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 MqTriggerRejectedApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class MqTriggerRejectedApplicationService implements MqTriggerRejectedFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final MqTriggerRejectedCatalog catalog;

    /**
     * 执行 MqTriggerRejectedApplicationService 对应的业务处理。
     */
    public MqTriggerRejectedApplicationService() {
        this(new MqTriggerRejectedCatalog());
    }

    /**
     * 执行 MqTriggerRejectedApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public MqTriggerRejectedApplicationService(MqTriggerRejectedCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public RejectedPageView list(RejectedQuery query) {
        return catalog.list(query);
    }

    /**
     * 执行 detail 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public RejectedView detail(Long id) {
        return catalog.detail(id);
    }

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public ReplayResult replay(Long id) {
        return catalog.replay(id);
    }

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    @Override
    public void register(RejectedCommand command) {
        catalog.register(command);
    }
}
