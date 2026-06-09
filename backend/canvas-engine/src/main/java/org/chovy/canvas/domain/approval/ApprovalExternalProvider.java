package org.chovy.canvas.domain.approval;

import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;

import java.util.List;

/**
 * ApprovalExternalProvider 定义 domain.approval 场景中的扩展契约。
 */
public interface ApprovalExternalProvider {

    /**
     * 判断业务条件是否成立。
     *
     * @param provider provider 参数，用于 supports 流程中的校验、计算或对象转换。
     * @return 返回 supports 的布尔判断结果。
     */
    boolean supports(String provider);

    /**
     * 执行核心业务处理流程。
     *
     * @param definition definition 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    ApprovalExternalSubmissionResult submit(ApprovalDefinitionDO definition,
                                            ApprovalInstanceDO instance,
                                            List<ApprovalTaskDO> tasks,
                                            ApprovalSubmitCommand command);

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param definition definition 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param task task 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param approve approve 参数，用于 decide 流程中的校验、计算或对象转换。
     */
    void decide(ApprovalDefinitionDO definition,
                ApprovalInstanceDO instance,
                ApprovalTaskDO task,
                ApprovalDecisionCommand command,
                boolean approve);

    /**
     * 执行核心业务处理流程。
     *
     * @param definition definition 参数，用于 sync 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 sync 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    default ApprovalExternalSyncResult sync(ApprovalDefinitionDO definition, ApprovalInstanceDO instance) {
        return null;
    }
}
