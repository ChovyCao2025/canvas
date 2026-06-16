package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.DlqFacade;
import org.chovy.canvas.execution.domain.DlqCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 DlqApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class DlqApplicationService implements DlqFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final DlqCatalog catalog;

    /**
     * 执行 DlqApplicationService 对应的业务处理。
     */
    public DlqApplicationService() {
        this(new DlqCatalog());
    }

    /**
     * 执行 DlqApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public DlqApplicationService(DlqCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 list 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public DlqPageView list(DlqQuery query) {
        return catalog.list(query);
    }

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @param skipSuccessNodes skipSuccessNodes 参数
     * @return 处理后的结果
     */
    @Override
    public DlqReplayResult replay(Long id, boolean skipSuccessNodes) {
        return catalog.replay(id, skipSuccessNodes);
    }

    /**
     * 执行 delete 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public DeleteResult delete(Long id) {
        return catalog.delete(id);
    }

    /**
     * 执行 register 对应的业务处理。
     * @param command command 参数
     */
    @Override
    public void register(DlqEntryCommand command) {
        catalog.register(command);
    }
}
