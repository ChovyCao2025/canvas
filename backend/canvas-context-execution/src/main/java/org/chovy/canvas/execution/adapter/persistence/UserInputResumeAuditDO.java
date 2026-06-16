package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 UserInputResumeAuditDO 的执行上下文数据结构或业务契约。
 */
@TableName("user_input_resume_audit")
public class UserInputResumeAuditDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId(type = IdType.AUTO)
    public Long id;

    /**
     * 保存 tenantId 对应的状态或配置。
     */
    public Long tenantId;

    /**
     * 保存 responseId 对应的状态或配置。
     */
    public Long responseId;

    /**
     * 保存 executionId 对应的状态或配置。
     */
    public String executionId;

    /**
     * 保存 nodeId 对应的状态或配置。
     */
    public String nodeId;

    /**
     * 保存 userId 对应的状态或配置。
     */
    public String userId;

    /**
     * 保存 resumeStatus 对应的状态或配置。
     */
    public String resumeStatus;

    /**
     * 保存 resumePayload 对应的状态或配置。
     */
    public String resumePayload;

    /**
     * 保存 createdAt 对应的状态或配置。
     */
    public LocalDateTime createdAt;
}
