package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.AnalyticsEventDO;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsEventMapper extends BaseMapper<AnalyticsEventDO> {

    @Select("""
            SELECT event_code AS eventCode, COUNT(*) AS count
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            GROUP BY event_code
            ORDER BY count DESC
            LIMIT 200
            """)
    List<Map<String, Object>> selectEventCounts(@Param("tenantId") Long tenantId,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    @Select("""
            SELECT event_code AS eventCode, DATE_FORMAT(event_time, '%Y-%m-%dT%H:%i:%s') AS eventTime
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            ORDER BY event_time DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Map<String, Object>> selectUserTimeline(@Param("tenantId") Long tenantId,
                                                 @Param("userId") String userId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate,
                                                 @Param("offset") int offset,
                                                 @Param("size") int size);

    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    long countUserTimeline(@Param("tenantId") Long tenantId,
                           @Param("userId") String userId,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    @Select("""
            SELECT COALESCE(JSON_UNQUOTE(JSON_EXTRACT(attributes_json, CONCAT('$.', #{attribute}))), '(empty)') AS value,
                   COUNT(*) AS count
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            GROUP BY value
            ORDER BY count DESC
            LIMIT 100
            """)
    List<Map<String, Object>> selectAttributeDistribution(@Param("tenantId") Long tenantId,
                                                          @Param("attribute") String attribute,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    long countEvents(@Param("tenantId") Long tenantId,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate);

    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_code = #{eventCode}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    long countByEventCode(@Param("tenantId") Long tenantId,
                          @Param("eventCode") String eventCode,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate);

    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND archive_status = 'ACTIVE'
              AND (#{skipLegalHold} = FALSE OR legal_hold = 0)
            """)
    long countRetentionEligible(@Param("tenantId") Long tenantId,
                                @Param("retentionDays") int retentionDays,
                                @Param("skipLegalHold") boolean skipLegalHold);

    @Update("""
            UPDATE analytics_event
            SET archive_status = 'ARCHIVED', archived_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND archive_status = 'ACTIVE'
              AND legal_hold = 0
            ORDER BY event_time ASC, id ASC
            LIMIT #{limit}
            """)
    int archiveRetentionBatch(@Param("tenantId") Long tenantId,
                              @Param("retentionDays") int retentionDays,
                              @Param("limit") int limit);

    @Delete("""
            DELETE FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND legal_hold = 0
            ORDER BY event_time ASC, id ASC
            LIMIT #{limit}
            """)
    int deleteRetentionBatch(@Param("tenantId") Long tenantId,
                             @Param("retentionDays") int retentionDays,
                             @Param("limit") int limit);
}
