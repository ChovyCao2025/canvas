package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseRealtimeRetryDO;

import java.time.LocalDateTime;

@Mapper
public interface CdpWarehouseRealtimeRetryMapper extends BaseMapper<CdpWarehouseRealtimeRetryDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_realtime_retry
            (tenant_id, event_log_id, message_id, event_code, status, attempt_count,
             first_error, last_error, next_retry_at)
            VALUES
            (#{row.tenantId}, #{row.eventLogId}, #{row.messageId}, #{row.eventCode}, #{row.status}, 0,
             #{row.firstError}, #{row.lastError}, #{row.nextRetryAt})
            ON DUPLICATE KEY UPDATE
                status = CASE WHEN status = 'SUCCESS' THEN status ELSE 'PENDING' END,
                last_error = VALUES(last_error),
                next_retry_at = VALUES(next_retry_at),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertPending(@Param("row") CdpWarehouseRealtimeRetryDO row);

    @Update("""
            UPDATE cdp_warehouse_realtime_retry
            SET status = 'SENDING',
                locked_by = #{workerId},
                locked_at = #{now},
                updated_at = #{now}
            WHERE id = #{id}
              AND status IN ('PENDING', 'RETRY')
              AND next_retry_at <= #{now}
            """)
    int claimDue(@Param("id") Long id, @Param("workerId") String workerId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_realtime_retry
            SET status = 'SUCCESS',
                locked_by = NULL,
                locked_at = NULL,
                last_error = NULL,
                next_retry_at = NULL,
                finished_at = #{now},
                updated_at = #{now}
            WHERE id = #{id}
            """)
    int markSuccess(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_realtime_retry
            SET status = 'RETRY',
                attempt_count = attempt_count + 1,
                locked_by = NULL,
                locked_at = NULL,
                last_error = #{errorMessage},
                next_retry_at = #{nextRetryAt},
                updated_at = #{now}
            WHERE id = #{id}
            """)
    int markRetry(@Param("id") Long id,
                  @Param("errorMessage") String errorMessage,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("now") LocalDateTime now);

    @Update("""
            UPDATE cdp_warehouse_realtime_retry
            SET status = 'DEAD',
                attempt_count = attempt_count + 1,
                locked_by = NULL,
                locked_at = NULL,
                last_error = #{errorMessage},
                next_retry_at = NULL,
                finished_at = #{now},
                updated_at = #{now}
            WHERE id = #{id}
            """)
    int markDead(@Param("id") Long id, @Param("errorMessage") String errorMessage, @Param("now") LocalDateTime now);
}
