package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;

@Mapper
public interface CdpWarehouseFieldPolicyMapper extends BaseMapper<CdpWarehouseFieldPolicyDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_field_policy
            (tenant_id, dataset_key, field_key, physical_name, column_name, value_type, semantic_type,
             pii_level, access_policy, min_role, allowed_usages, mask_strategy, lifecycle_status,
             owner_name, description)
            VALUES
            (#{row.tenantId}, #{row.datasetKey}, #{row.fieldKey}, #{row.physicalName}, #{row.columnName},
             #{row.valueType}, #{row.semanticType}, #{row.piiLevel}, #{row.accessPolicy}, #{row.minRole},
             #{row.allowedUsages}, #{row.maskStrategy}, #{row.lifecycleStatus}, #{row.ownerName},
             #{row.description})
            ON DUPLICATE KEY UPDATE
                physical_name = VALUES(physical_name),
                column_name = VALUES(column_name),
                value_type = VALUES(value_type),
                semantic_type = VALUES(semantic_type),
                pii_level = VALUES(pii_level),
                access_policy = VALUES(access_policy),
                min_role = VALUES(min_role),
                allowed_usages = VALUES(allowed_usages),
                mask_strategy = VALUES(mask_strategy),
                lifecycle_status = VALUES(lifecycle_status),
                owner_name = VALUES(owner_name),
                description = VALUES(description),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseFieldPolicyDO row);
}
