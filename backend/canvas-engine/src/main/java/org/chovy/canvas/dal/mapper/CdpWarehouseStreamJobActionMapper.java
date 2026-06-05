package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobActionDO;

import java.time.LocalDateTime;
import java.util.List;

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
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("actionId") Long actionId,
                     @Param("status") String status,
                     @Param("acknowledgedAt") LocalDateTime acknowledgedAt,
                     @Param("completedAt") LocalDateTime completedAt,
                     @Param("resultMessage") String resultMessage);
}
