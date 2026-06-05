package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseTableContractDO;

import java.time.LocalDateTime;

@Mapper
public interface CdpWarehouseTableContractMapper extends BaseMapper<CdpWarehouseTableContractDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_table_contract
            (tenant_id, table_key, dataset_key, layer, physical_name, engine_type, ddl_asset_path,
             partition_column, partition_granularity, retention_days, replica_count, bucket_count,
             distribution_columns, storage_policy, lifecycle_status, owner_name, description, expected_properties_json)
            VALUES
            (#{row.tenantId}, #{row.tableKey}, #{row.datasetKey}, #{row.layer}, #{row.physicalName},
             #{row.engineType}, #{row.ddlAssetPath}, #{row.partitionColumn}, #{row.partitionGranularity},
             #{row.retentionDays}, #{row.replicaCount}, #{row.bucketCount}, #{row.distributionColumns},
             #{row.storagePolicy}, #{row.lifecycleStatus}, #{row.ownerName}, #{row.description},
             #{row.expectedPropertiesJson})
            ON DUPLICATE KEY UPDATE
                dataset_key = VALUES(dataset_key),
                layer = VALUES(layer),
                physical_name = VALUES(physical_name),
                engine_type = VALUES(engine_type),
                ddl_asset_path = VALUES(ddl_asset_path),
                partition_column = VALUES(partition_column),
                partition_granularity = VALUES(partition_granularity),
                retention_days = VALUES(retention_days),
                replica_count = VALUES(replica_count),
                bucket_count = VALUES(bucket_count),
                distribution_columns = VALUES(distribution_columns),
                storage_policy = VALUES(storage_policy),
                lifecycle_status = VALUES(lifecycle_status),
                owner_name = VALUES(owner_name),
                description = VALUES(description),
                expected_properties_json = VALUES(expected_properties_json),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseTableContractDO row);

    @Update("""
            UPDATE cdp_warehouse_table_contract
            SET last_inspected_at = #{inspectedAt},
                last_inspection_status = #{status},
                last_inspection_message = #{message},
                updated_at = #{inspectedAt}
            WHERE tenant_id = #{tenantId}
              AND table_key = #{tableKey}
            """)
    int updateInspection(@Param("tenantId") Long tenantId,
                         @Param("tableKey") String tableKey,
                         @Param("inspectedAt") LocalDateTime inspectedAt,
                         @Param("status") String status,
                         @Param("message") String message);
}
