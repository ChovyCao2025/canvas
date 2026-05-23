package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface CanvasUserQuotaMapper extends BaseMapper<CanvasUserQuota> {

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
    int upsertUsage(@Param("canvasId") Long canvasId,
                    @Param("userId") String userId,
                    @Param("triggerDate") LocalDate triggerDate,
                    @Param("lastTriggerAt") LocalDateTime lastTriggerAt);
}
