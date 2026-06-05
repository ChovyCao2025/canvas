package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.AnalyticsEventTraceDO;

@Mapper
public interface AnalyticsEventTraceMapper extends BaseMapper<AnalyticsEventTraceDO> {

    @Select("""
            SELECT COUNT(*)
            FROM analytics_event_trace
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND archive_status = 'ACTIVE'
              AND (#{skipLegalHold} = FALSE OR legal_hold = 0)
            """)
    long countRetentionEligible(@Param("tenantId") Long tenantId,
                                @Param("retentionDays") int retentionDays,
                                @Param("skipLegalHold") boolean skipLegalHold);

    @Update("""
            UPDATE analytics_event_trace
            SET archive_status = 'ARCHIVED', archived_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND archive_status = 'ACTIVE'
              AND legal_hold = 0
            ORDER BY event_time ASC, id ASC
            LIMIT #{limit}
            """)
    int archiveRetentionBatch(@Param("tenantId") Long tenantId,
                              @Param("retentionDays") int retentionDays,
                              @Param("limit") int limit);

    @Delete("""
            DELETE FROM analytics_event_trace
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND legal_hold = 0
            ORDER BY event_time ASC, id ASC
            LIMIT #{limit}
            """)
    int deleteRetentionBatch(@Param("tenantId") Long tenantId,
                             @Param("retentionDays") int retentionDays,
                             @Param("limit") int limit);
}
