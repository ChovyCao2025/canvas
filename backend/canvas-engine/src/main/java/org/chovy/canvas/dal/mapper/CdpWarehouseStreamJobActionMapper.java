package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobActionDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseStreamJobActionMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseStreamJobActionMapper extends BaseMapper<CdpWarehouseStreamJobActionDO> {

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, action, status, requested_by, reason,
                   requested_at, acknowledged_at, completed_at, result_message, created_at, updated_at
            FROM cdp_warehouse_stream_job_action
            WHERE tenant_id = #{tenantId}
              AND pipeline_key = #{pipelineKey}
              AND job_key = #{jobKey}
              AND status = 'PENDING'
            ORDER BY requested_at ASC, id ASC
            LIMIT #{limit}
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseStreamJobActionDO> selectPending(@Param("tenantId") Long tenantId,
                                                      @Param("pipelineKey") String pipelineKey,
                                                      @Param("jobKey") String jobKey,
                                                      @Param("limit") int limit);

    @Select("""
            SELECT id, tenant_id, pipeline_key, job_key, action, status, requested_by, reason,
                   requested_at, acknowledged_at, completed_at, result_message, created_at, updated_at
            FROM cdp_warehouse_stream_job_action
            WHERE tenant_id = #{tenantId}
              AND id = #{actionId}
            LIMIT 1
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actionId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    CdpWarehouseStreamJobActionDO findByTenantAndId(@Param("tenantId") Long tenantId,
                                                    @Param("actionId") Long actionId);

    @Update("""
            UPDATE cdp_warehouse_stream_job_action
            SET status = #{status},
                acknowledged_at = #{acknowledgedAt},
                completed_at = #{completedAt},
                result_message = #{resultMessage},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{actionId}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actionId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param acknowledgedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param completedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param resultMessage result message 参数，用于 updateStatus 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("actionId") Long actionId,
                     @Param("status") String status,
                     @Param("acknowledgedAt") LocalDateTime acknowledgedAt,
                     @Param("completedAt") LocalDateTime completedAt,
                     @Param("resultMessage") String resultMessage);
}
