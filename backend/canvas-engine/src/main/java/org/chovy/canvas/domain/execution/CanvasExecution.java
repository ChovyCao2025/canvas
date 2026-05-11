package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("canvas_execution")
public class CanvasExecution {

    @TableId
    private String id;          // UUID

    private Long   canvasId;
    private Long   versionId;
    private String userId;
    private String triggerType; // MQ / DIRECT_CALL / BEHAVIOR / DRY_RUN

    /** 0执行中 1暂停 2成功 3失败 */
    private Integer status;

    private String result;         // 执行结果 JSON
    private String lastDedupKey;   // Watchdog 用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
