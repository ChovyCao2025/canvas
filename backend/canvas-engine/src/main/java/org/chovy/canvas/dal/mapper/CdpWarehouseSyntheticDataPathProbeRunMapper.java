package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyntheticDataPathProbeRunDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CdpWarehouseSyntheticDataPathProbeRunMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseSyntheticDataPathProbeRunMapper
        extends BaseMapper<CdpWarehouseSyntheticDataPathProbeRunDO> {

    @Update("""
            UPDATE cdp_warehouse_synthetic_data_path_probe_run
            SET status = #{row.status},
                source_status = #{row.sourceStatus},
                sink_status = #{row.sinkStatus},
                ods_status = #{row.odsStatus},
                ods_row_count = #{row.odsRowCount},
                finished_at = #{row.finishedAt},
                error_message = #{row.errorMessage},
                evidence_json = #{row.evidenceJson},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{row.tenantId}
              AND id = #{row.id}
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int updateCompletion(@Param("row") CdpWarehouseSyntheticDataPathProbeRunDO row);

    @Select("""
            SELECT id, tenant_id, probe_key, message_id, event_code, user_id, strict_mode,
                   source_mode, status, source_status, sink_status, ods_status, ods_row_count, started_at, finished_at,
                   error_message, evidence_json, created_at, updated_at
            FROM cdp_warehouse_synthetic_data_path_probe_run
            WHERE tenant_id = #{tenantId}
            ORDER BY started_at DESC, id DESC
            LIMIT #{limit}
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseSyntheticDataPathProbeRunDO> listRecent(@Param("tenantId") Long tenantId,
                                                             @Param("limit") int limit);
}
