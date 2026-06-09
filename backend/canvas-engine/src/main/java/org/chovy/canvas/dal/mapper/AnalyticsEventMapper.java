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

/**
 * AnalyticsEventMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface AnalyticsEventMapper extends BaseMapper<AnalyticsEventDO> {

            /**
             * 查询或读取业务数据。
             *
             * @param endDate 时间参数，用于计算窗口、过期或审计时间。
             * @return 返回 COUNT 流程生成的业务结果。
             */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    List<Map<String, Object>> selectEventCounts(@Param("tenantId") Long tenantId,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

            /**
             * 执行 DATE_FORMAT 流程，围绕 date format 完成校验、计算或结果组装。
             *
             * @param event_time 时间参数，用于计算窗口、过期或审计时间。
             * @param size 分页或数量限制，避免一次处理过多数据。
             * @return 返回 DATE_FORMAT 流程生成的业务结果。
             */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @param offset 分页或数量限制，避免一次处理过多数据。
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<Map<String, Object>> selectUserTimeline(@Param("tenantId") Long tenantId,
                                                 @Param("userId") String userId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate,
                                                 @Param("offset") int offset,
                                                 @Param("size") int size);

            /**
             * 查询或读取业务数据。
             *
             * @param endDate 时间参数，用于计算窗口、过期或审计时间。
             * @return 返回 COUNT 流程生成的业务结果。
             */
    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回统计数量。
     */
    long countUserTimeline(@Param("tenantId") Long tenantId,
                           @Param("userId") String userId,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate);

            /**
             * 执行 COALESCE 流程，围绕 coalesce 完成校验、计算或结果组装。
             *
             * @param value 待处理值，用于规则计算或转换。
             * @param endDate 时间参数，用于计算窗口、过期或审计时间。
             * @return 返回 COALESCE 流程生成的业务结果。
             */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attribute attribute 参数，用于 selectAttributeDistribution 流程中的校验、计算或对象转换。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    List<Map<String, Object>> selectAttributeDistribution(@Param("tenantId") Long tenantId,
                                                          @Param("attribute") String attribute,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

            /**
             * 查询或读取业务数据。
             *
             * @param endDate 时间参数，用于计算窗口、过期或审计时间。
             * @return 返回 COUNT 流程生成的业务结果。
             */
    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回统计数量。
     */
    long countEvents(@Param("tenantId") Long tenantId,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate);

            /**
             * 查询或读取业务数据。
             *
             * @param endDate 时间参数，用于计算窗口、过期或审计时间。
             * @return 返回 COUNT 流程生成的业务结果。
             */
    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND event_code = #{eventCode}
              AND event_time >= #{startDate}
              AND event_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)
              AND archive_status = 'ACTIVE'
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回统计数量。
     */
    long countByEventCode(@Param("tenantId") Long tenantId,
                          @Param("eventCode") String eventCode,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate);

            /**
             * 查询或读取业务数据。
             *
             * @param skipLegalHold skip legal hold 参数，用于 COUNT 流程中的校验、计算或对象转换。
             * @return 返回 COUNT 流程生成的业务结果。
             */
    @Select("""
            SELECT COUNT(*)
            FROM analytics_event
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
              AND archive_status = 'ACTIVE'
              AND (#{skipLegalHold} = FALSE OR legal_hold = 0)
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param retentionDays retention days 参数，用于 countRetentionEligible 流程中的校验、计算或对象转换。
     * @param skipLegalHold skip legal hold 参数，用于 countRetentionEligible 流程中的校验、计算或对象转换。
     * @return 返回统计数量。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param retentionDays retention days 参数，用于 archiveRetentionBatch 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 archive retention batch 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param retentionDays retention days 参数，用于 deleteRetentionBatch 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 delete retention batch 计算得到的数量、金额或指标值。
     */
    int deleteRetentionBatch(@Param("tenantId") Long tenantId,
                             @Param("retentionDays") int retentionDays,
                             @Param("limit") int limit);
}
