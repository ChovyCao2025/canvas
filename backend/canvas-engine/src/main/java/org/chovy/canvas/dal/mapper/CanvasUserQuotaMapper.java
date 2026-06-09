package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 画布用户配额 MyBatis-Plus Mapper。
 *
 * <p>继承 BaseMapper 为 {@code CanvasUserQuotaDO} 提供基础 CRUD 能力，复杂查询可在同名 XML 中扩展。
 * <p>该接口只定义数据访问边界，不承载业务编排或跨表事务逻辑。
 */
@Mapper
public interface CanvasUserQuotaMapper extends BaseMapper<CanvasUserQuotaDO> {

    @Insert("""
            INSERT INTO canvas_user_quota
                (canvas_id, user_id, trigger_date, daily_count, total_count, last_trigger_at)
            VALUES
                (#{canvasId}, #{userId}, #{triggerDate}, 1, 1, #{lastTriggerAt})
            ON DUPLICATE KEY UPDATE
                daily_count = daily_count + 1,
                total_count = total_count + 1,
                last_trigger_at = VALUES(last_trigger_at)
            """)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param triggerDate 时间参数，用于计算窗口、过期或审计时间。
     * @param lastTriggerAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    int upsertUsage(@Param("canvasId") Long canvasId,
                    @Param("userId") String userId,
                    @Param("triggerDate") LocalDate triggerDate,
                    @Param("lastTriggerAt") LocalDateTime lastTriggerAt);
}
