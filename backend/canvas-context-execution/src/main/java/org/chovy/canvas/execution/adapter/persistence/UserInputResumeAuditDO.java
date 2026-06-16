package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user_input_resume_audit")
public class UserInputResumeAuditDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public Long tenantId;
    public Long responseId;
    public String executionId;
    public String nodeId;
    public String userId;
    public String resumeStatus;
    public String resumePayload;
    public LocalDateTime createdAt;
}
