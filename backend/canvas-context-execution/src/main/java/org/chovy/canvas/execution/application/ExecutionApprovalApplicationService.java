package org.chovy.canvas.execution.application;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade;
import org.chovy.canvas.execution.domain.ExecutionApprovalCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 ExecutionApprovalApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class ExecutionApprovalApplicationService implements ExecutionApprovalFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final ExecutionApprovalCatalog catalog;

    /**
     * 执行 ExecutionApprovalApplicationService 对应的业务处理。
     */
    public ExecutionApprovalApplicationService() {
        this(new ExecutionApprovalCatalog());
    }

    /**
     * 执行 ExecutionApprovalApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    ExecutionApprovalApplicationService(ExecutionApprovalCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 approve 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param actor actor 参数
     * @param role role 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionApprovalDecision approve(Long tenantId, String executionId, String actor, String role) {
        return catalog.decide(tenantId, executionId, actor, role, null, true);
    }

    /**
     * 执行 reject 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param actor actor 参数
     * @param reason reason 参数
     * @param role role 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionApprovalDecision reject(Long tenantId, String executionId, String actor, String reason, String role) {
        return catalog.decide(tenantId, executionId, actor, role, reason, false);
    }
}
