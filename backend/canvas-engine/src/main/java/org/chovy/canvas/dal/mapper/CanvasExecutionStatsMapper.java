package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 画布执行统计 MyBatis-Plus Mapper。
 *
 * <p>继承 BaseMapper 为 {@code CanvasExecutionStatsDO} 提供基础 CRUD 能力，复杂查询可在同名 XML 中扩展。
 * <p>该接口只定义数据访问边界，不承载业务编排或跨表事务逻辑。
 */
@Mapper
public interface CanvasExecutionStatsMapper extends BaseMapper<CanvasExecutionStatsDO> {

    @Insert("""
            INSERT INTO canvas_execution_stats
                (canvas_id, stat_date, total_count, success_count, fail_count,
                 paused_count, timeout_count, unique_users)
            VALUES
                (#{canvasId}, #{statDate}, 1,
                 CASE WHEN #{status} = 2 THEN 1 ELSE 0 END,
                 CASE WHEN #{status} = 3 THEN 1 ELSE 0 END,
                 CASE WHEN #{status} = 1 THEN 1 ELSE 0 END,
                 0, 0)
            ON DUPLICATE KEY UPDATE
                total_count = total_count + 1,
                success_count = success_count + VALUES(success_count),
                fail_count = fail_count + VALUES(fail_count),
                paused_count = paused_count + VALUES(paused_count)
            """)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param statDate 时间参数，用于计算窗口、过期或审计时间。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回流程执行后的业务结果。
     */
    int upsertDailyIncrement(@Param("canvasId") Long canvasId,
                             @Param("statDate") LocalDate statDate,
                             @Param("status") int status);

    @Select("""
            SELECT id, canvas_id, stat_date, total_count, success_count, fail_count,
                   paused_count, timeout_count, unique_users, avg_duration_ms, p99_duration_ms
            FROM canvas_execution_stats
            WHERE canvas_id = #{canvasId}
              AND stat_date BETWEEN #{sinceDate} AND #{untilDate}
            ORDER BY stat_date ASC
            """)
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param sinceDate 时间参数，用于计算窗口、过期或审计时间。
     * @param untilDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CanvasExecutionStatsDO> selectByCanvasIdAndDateRange(@Param("canvasId") Long canvasId,
                                                              @Param("sinceDate") LocalDate sinceDate,
                                                              @Param("untilDate") LocalDate untilDate);
}
