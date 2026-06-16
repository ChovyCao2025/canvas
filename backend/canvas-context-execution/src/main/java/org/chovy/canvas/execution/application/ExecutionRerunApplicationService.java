package org.chovy.canvas.execution.application;

import java.util.List;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;
import org.chovy.canvas.execution.domain.ExecutionRerunCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定义 ExecutionRerunApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionRerunApplicationService implements ExecutionRerunFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final ExecutionRerunCatalog catalog;

    /**
     * 执行 ExecutionRerunApplicationService 对应的业务处理。
     */
    public ExecutionRerunApplicationService() {
        this(new ExecutionRerunCatalog());
    }

    /**
     * 执行 ExecutionRerunApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public ExecutionRerunApplicationService(ExecutionRerunCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 rerun 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param operator operator 参数
     * @param admin admin 参数
     * @param canvasId canvasId 参数
     * @param command command 参数
     * @return 处理后的结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId, RerunCommand command) {
        return catalog.rerun(tenantId, operator, admin, canvasId, command);
    }

    /**
     * 执行 audit 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public AuditRow audit(Long tenantId, Long id) {
        return catalog.audit(tenantId, id);
    }

    /**
     * 执行 audits 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @return 处理后的结果
     */
    @Override
    public List<AuditRow> audits(Long tenantId, Long canvasId) {
        return catalog.audits(tenantId, canvasId);
    }
}
