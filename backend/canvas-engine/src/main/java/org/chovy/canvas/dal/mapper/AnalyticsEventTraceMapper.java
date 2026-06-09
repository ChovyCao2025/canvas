package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.AnalyticsEventTraceDO;

/**
 * AnalyticsEventTraceMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface AnalyticsEventTraceMapper extends BaseMapper<AnalyticsEventTraceDO> {

            /**
             * 查询或读取业务数据。
             *
             * @param skipLegalHold skip legal hold 参数，用于 COUNT 流程中的校验、计算或对象转换。
             * @return 返回 COUNT 流程生成的业务结果。
             */
    @Select("""
            SELECT COUNT(*)
            FROM analytics_event_trace
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
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
            UPDATE analytics_event_trace
            SET archive_status = 'ARCHIVED', archived_at = NOW()
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
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
            DELETE FROM analytics_event_trace
            WHERE tenant_id = #{tenantId}
              AND COALESCE(event_time, finished_at, created_at) < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
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
