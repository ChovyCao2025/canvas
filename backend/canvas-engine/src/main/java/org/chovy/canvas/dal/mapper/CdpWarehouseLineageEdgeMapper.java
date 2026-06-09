package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseLineageEdgeDO;

/**
 * CdpWarehouseLineageEdgeMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseLineageEdgeMapper extends BaseMapper<CdpWarehouseLineageEdgeDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_lineage_edge
            (tenant_id, upstream_dataset_key, downstream_dataset_key, transform_type, transform_ref,
             dependency_type, description, active)
            VALUES
            (#{row.tenantId}, #{row.upstreamDatasetKey}, #{row.downstreamDatasetKey}, #{row.transformType},
             #{row.transformRef}, #{row.dependencyType}, #{row.description}, #{row.active})
            ON DUPLICATE KEY UPDATE
                transform_type = VALUES(transform_type),
                dependency_type = VALUES(dependency_type),
                description = VALUES(description),
                active = VALUES(active),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseLineageEdgeDO row);
}
