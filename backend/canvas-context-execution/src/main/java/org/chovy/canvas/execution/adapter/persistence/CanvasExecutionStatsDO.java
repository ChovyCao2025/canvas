package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_execution_stats")
public class CanvasExecutionStatsDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public Long canvasId;
    public LocalDate statDate;
    public Integer totalCount;
    public Integer successCount;
    public Integer failCount;
    public Integer pausedCount;
    public Integer timeoutCount;
    public Integer uniqueUsers;
    public Long avgDurationMs;
    public Long p99DurationMs;
}
