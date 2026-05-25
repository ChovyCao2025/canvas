package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 画布用户配额记录（canvas_user_quota）。
 *
 * <p>用途：
 * <ol>
 *   <li>为 {@link TriggerPreCheckService#checkWithoutQuotaAccounting} 提供冷却期软检查依据
 *       （读取 lastTriggerAt，判断距上次触发是否已过冷却期）；</li>
 *   <li>运营后台统计看板（每日/累计触发量查询）。</li>
 * </ol>
 *
 * <p>写入方式：{@code updateQuotaAsync} 通过虚拟线程异步 UPSERT，不阻塞主流程。
 * 因此数据有延迟，不可作为强一致配额防线（强防线在 Redis INCR/SETNX 层）。
 *
 * <p>主键：(canvas_id, user_id, trigger_date) 复合主键，按日分桶，无 @TableId。
 */
@Data
@TableName("canvas_user_quota")
public class CanvasUserQuotaDO {

    /** 画布 ID。 */
    private Long canvasId;

    /** 用户 ID。 */
    private String userId;

    /**
     * 触发日期（自然日，按 JVM 本地时区），用于每日用量的分桶统计。
     * 跨日触发自动产生新行（UPSERT 以 trigger_date 为条件）。
     */
    private LocalDate triggerDate;

    /**
     * 当日触发次数。
     * 由 {@code upsertUsage} 异步累加，可能比 Redis perUserDailyLimit 计数略少（延迟写）。
     */
    private Integer dailyCount;

    /**
     * 当日累计总触发次数（当前实现以天为粒度分桶，跨多天的累计需在应用层汇总）。
     * 仅供统计看板参考，不用于强配额判断。
     */
    private Integer totalCount;

    /**
     * 最近一次触发时间（用于冷却期软检查）。
     * 由 {@code upsertUsage} 异步写入，可能滞后于实际触发时间 100ms~几秒。
     * Redis 冷却 key（SETNX）是真正的原子防线，此字段仅作预检优化。
     */
    private LocalDateTime lastTriggerAt;
}
