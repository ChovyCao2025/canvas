package org.chovy.canvas.domain.approval;

import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;

/**
 * ApprovalAutoActionHandler 定义 domain.approval 场景中的扩展契约。
 */
public interface ApprovalAutoActionHandler {

    /**
     * 判断业务条件是否成立。
     *
     * @param autoAction auto action 参数，用于 supports 流程中的校验、计算或对象转换。
     * @return 返回 supports 的布尔判断结果。
     */
    boolean supports(String autoAction);

    /**
     * 判断业务条件是否成立。
     *
     * @param autoAction auto action 参数，用于 supportsStatus 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 supports status 的布尔判断结果。
     */
    default boolean supportsStatus(String autoAction, String status) {
        return ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param instance instance 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    void execute(ApprovalInstanceDO instance, String actor);
}
