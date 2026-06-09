package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseDatasetCatalogDO;

/**
 * CdpWarehouseDatasetCatalogMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseDatasetCatalogMapper extends BaseMapper<CdpWarehouseDatasetCatalogDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_dataset_catalog
            (tenant_id, dataset_key, layer, physical_name, display_name, subject_area, source_system, owner_name,
             description, freshness_sla_minutes, pii_level, status, schema_json)
            VALUES
            (#{row.tenantId}, #{row.datasetKey}, #{row.layer}, #{row.physicalName}, #{row.displayName},
             #{row.subjectArea}, #{row.sourceSystem}, #{row.ownerName}, #{row.description},
             #{row.freshnessSlaMinutes}, #{row.piiLevel}, #{row.status}, #{row.schemaJson})
            ON DUPLICATE KEY UPDATE
                layer = VALUES(layer),
                physical_name = VALUES(physical_name),
                display_name = VALUES(display_name),
                subject_area = VALUES(subject_area),
                source_system = VALUES(source_system),
                owner_name = VALUES(owner_name),
                description = VALUES(description),
                freshness_sla_minutes = VALUES(freshness_sla_minutes),
                pii_level = VALUES(pii_level),
                status = VALUES(status),
                schema_json = VALUES(schema_json),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseDatasetCatalogDO row);
}
