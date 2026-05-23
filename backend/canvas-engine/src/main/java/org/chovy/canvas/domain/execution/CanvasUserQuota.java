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

    /** 画布 ID。 */
    private Long canvasId;

    /** 用户 ID。 */
    private String userId;

    /** 触发日期（日粒度统计分桶）。 */
    private LocalDate triggerDate;

    /** 当日触发次数。 */
    private Integer dailyCount;

    /** 当日累计总触发次数（当前实现按日分桶）。 */
    private Integer totalCount;

    /** 最近一次触发时间。 */
    private LocalDateTime lastTriggerAt;
}
