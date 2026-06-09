package org.chovy.canvas.domain.approval;

/**
 * LarkApprovalClient 定义 domain.approval 场景中的扩展契约。
 */
public interface LarkApprovalClient {

    /**
     * 执行数据写入或状态变更。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    String createInstance(LarkApprovalCreateInstanceRequest request);

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param instanceCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 getInstance 流程生成的业务结果。
     */
    LarkApprovalInstanceSnapshot getInstance(Long tenantId, String instanceCode);

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     */
    void approveTask(LarkApprovalTaskActionRequest request);

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     */
    void rejectTask(LarkApprovalTaskActionRequest request);
}
