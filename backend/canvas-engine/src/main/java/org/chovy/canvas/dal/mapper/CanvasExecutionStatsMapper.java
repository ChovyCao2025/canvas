package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

import java.time.LocalDate;

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
    int upsertDailyIncrement(@Param("canvasId") Long canvasId,
                             @Param("statDate") LocalDate statDate,
                             @Param("status") int status);
}
