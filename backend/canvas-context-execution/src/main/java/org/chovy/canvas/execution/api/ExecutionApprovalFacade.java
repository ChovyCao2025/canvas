package org.chovy.canvas.execution.api;

import java.time.LocalDateTime;

/**
 * 定义 ExecutionApprovalFacade 的执行上下文数据结构或业务契约。
 */
public interface ExecutionApprovalFacade {

    /**
     * 执行 approve 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param actor actor 参数
     * @param role role 参数
     * @return 处理后的结果
     */
    ExecutionApprovalDecision approve(Long tenantId, String executionId, String actor, String role);

    /**
     * 执行 reject 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param actor actor 参数
     * @param reason reason 参数
     * @param role role 参数
     * @return 处理后的结果
     */
    ExecutionApprovalDecision reject(Long tenantId, String executionId, String actor, String reason, String role);

    /**
     * 定义 ExecutionApprovalDecision 的执行上下文数据结构或业务契约。
     * @param executionId executionId 对应的数据字段
     * @param result result 对应的数据字段
     * @param resultBy resultBy 对应的数据字段
     * @param comment comment 对应的数据字段
     * @param resultAt resultAt 对应的数据字段
     */
    record ExecutionApprovalDecision(
            String executionId,
            String result,
            String resultBy,
            String comment,
            LocalDateTime resultAt) {
    }
}
