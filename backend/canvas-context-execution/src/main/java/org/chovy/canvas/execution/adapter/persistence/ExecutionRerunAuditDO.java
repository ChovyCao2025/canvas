package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 ExecutionRerunAuditDO 的执行上下文数据结构或业务契约。
 */
@TableName("execution_rerun_audit")
public class ExecutionRerunAuditDO {

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
     * 保存 sourceExecutionId 对应的状态或配置。
     */
    public String sourceExecutionId;

    /**
     * 保存 rerunExecutionId 对应的状态或配置。
     */
    public String rerunExecutionId;

    /**
     * 保存 canvasId 对应的状态或配置。
     */
    public Long canvasId;

    /**
     * 保存 versionId 对应的状态或配置。
     */
    public Long versionId;

    /**
     * 保存 operator 对应的状态或配置。
     */
    public String operator;

    /**
     * 保存 reason 对应的状态或配置。
     */
    public String reason;

    /**
     * 保存 payloadJson 对应的状态或配置。
     */
    public String payloadJson;

    /**
     * 保存 createdAt 对应的状态或配置。
     */
    public LocalDateTime createdAt;
}
