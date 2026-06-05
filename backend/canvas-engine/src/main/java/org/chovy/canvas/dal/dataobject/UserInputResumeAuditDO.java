package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_input_resume_audit")
public class UserInputResumeAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long responseId;

    private String executionId;

    private String nodeId;

    private String userId;

    private String resumeStatus;

    private String resumePayload;

    private LocalDateTime createdAt;
}
