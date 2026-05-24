package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * 画布每日执行统计（canvas_execution_stats）。
 *
 * <p>以 (canvas_id, stat_date) 为联合唯一键，记录画布当日的聚合指标。
 * 由执行引擎在每次执行完成后异步 upsert，用于统计大盘展示和告警。
 */
@Data
@TableName("canvas_execution_stats")
public class CanvasExecutionStatsDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 画布 ID */
    private Long canvasId;

    /** 统计日期 */
    private LocalDate statDate;

    /** 当日触发总次数 */
    private Integer totalCount;

    /** 当日成功次数（status = SUCCESS） */
    private Integer successCount;

    /** 当日失败次数（status = FAILED） */
    private Integer failCount;

    /** 当日挂起次数（status = PAUSED，等待人工审批等） */
    private Integer pausedCount;

    /** 当日超时次数（执行超过 global-timeout-sec） */
    private Integer timeoutCount;

    /** 当日触发的独立用户数（去重） */
    private Integer uniqueUsers;

    /** 当日成功执行的平均耗时（毫秒），null 表示无成功记录 */
    private Long avgDurationMs;

    /** 当日成功执行的 P99 耗时（毫秒），null 表示无成功记录 */
    private Long p99DurationMs;
}
