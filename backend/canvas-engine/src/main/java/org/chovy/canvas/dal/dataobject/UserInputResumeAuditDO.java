package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserInputResumeAuditDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("user_input_resume_audit")
public class UserInputResumeAuditDO {

    /** 用户输入恢复审计主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的响应 ID */
    private Long responseId;

    /** 关联的执行 ID */
    private String executionId;

    /** 关联的节点 ID */
    private String nodeId;

    /** 关联的用户 ID */
    private String userId;

    /** 用户输入恢复审计恢复状态 */
    private String resumeStatus;

    /** 用户输入恢复审计恢复载荷 */
    private String resumePayload;

    /** 用户输入恢复审计创建时间 */
    private LocalDateTime createdAt;
}
