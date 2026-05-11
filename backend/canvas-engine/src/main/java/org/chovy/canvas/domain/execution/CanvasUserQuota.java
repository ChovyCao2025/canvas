package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复合主键 (canvas_id, user_id, trigger_date)。
 * 不使用 @TableId，统一用 selectOne() + update(entity, wrapper) 操作。
 */
@Data
@TableName("canvas_user_quota")
public class CanvasUserQuota {

    private Long      canvasId;
    private String    userId;
    private LocalDate triggerDate;
    private Integer   dailyCount;
    private Integer   totalCount;
    private LocalDateTime lastTriggerAt;
}
