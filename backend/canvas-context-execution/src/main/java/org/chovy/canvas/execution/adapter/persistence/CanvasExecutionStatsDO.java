package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasExecutionStatsDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_execution_stats")
public class CanvasExecutionStatsDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId(type = IdType.AUTO)
    public Long id;

    /**
     * 保存 canvasId 对应的状态或配置。
     */
    public Long canvasId;

    /**
     * 保存 statDate 对应的状态或配置。
     */
    public LocalDate statDate;

    /**
     * 保存 totalCount 对应的状态或配置。
     */
    public Integer totalCount;

    /**
     * 保存 successCount 对应的状态或配置。
     */
    public Integer successCount;

    /**
     * 保存 failCount 对应的状态或配置。
     */
    public Integer failCount;

    /**
     * 保存 pausedCount 对应的状态或配置。
     */
    public Integer pausedCount;

    /**
     * 保存 timeoutCount 对应的状态或配置。
     */
    public Integer timeoutCount;

    /**
     * 保存 uniqueUsers 对应的状态或配置。
     */
    public Integer uniqueUsers;

    /**
     * 保存 avgDurationMs 对应的状态或配置。
     */
    public Long avgDurationMs;

    /**
     * 保存 p99DurationMs 对应的状态或配置。
     */
    public Long p99DurationMs;
}
