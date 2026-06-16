package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasExecutionTraceDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_execution_trace")
public class CanvasExecutionTraceDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId(type = IdType.AUTO)
    public Long id;

    /**
     * 保存 tenantId 对应的状态或配置。
     */
    @TableField("tenant_id")
    public Long tenantId;

    /**
     * 保存 executionId 对应的状态或配置。
     */
    public String executionId;

    /**
     * 保存 nodeId 对应的状态或配置。
     */
    public String nodeId;

    /**
     * 保存 nodeType 对应的状态或配置。
     */
    public String nodeType;

    /**
     * 保存 nodeName 对应的状态或配置。
     */
    public String nodeName;

    /**
     * 保存 status 对应的状态或配置。
     */
    public Integer status;

    /**
     * 保存 inputData 对应的状态或配置。
     */
    public String inputData;

    /**
     * 保存 outputData 对应的状态或配置。
     */
    public String outputData;

    /**
     * 保存 errorMsg 对应的状态或配置。
     */
    public String errorMsg;

    /**
     * 保存 startedAt 对应的状态或配置。
     */
    public LocalDateTime startedAt;

    /**
     * 保存 finishedAt 对应的状态或配置。
     */
    public LocalDateTime finishedAt;

    /**
     * 保存 durationMs 对应的状态或配置。
     */
    public Long durationMs;
}
