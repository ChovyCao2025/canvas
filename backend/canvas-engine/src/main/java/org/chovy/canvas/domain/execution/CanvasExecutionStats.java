package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("canvas_execution_stats")
public class CanvasExecutionStats {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long    canvasId;
    private LocalDate statDate;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer pausedCount;
    private Integer timeoutCount;
    private Integer uniqueUsers;
    private Long    avgDurationMs;
    private Long    p99DurationMs;
}
