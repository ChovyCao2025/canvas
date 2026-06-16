package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("execution_rerun_audit")
public class ExecutionRerunAuditDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public Long tenantId;
    public String sourceExecutionId;
    public String rerunExecutionId;
    public Long canvasId;
    public Long versionId;
    public String operator;
    public String reason;
    public String payloadJson;
    public LocalDateTime createdAt;
}
