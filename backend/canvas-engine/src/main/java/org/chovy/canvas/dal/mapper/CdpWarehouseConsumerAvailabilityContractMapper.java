package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseConsumerAvailabilityContractDO;

import java.time.LocalDateTime;

@Mapper
public interface CdpWarehouseConsumerAvailabilityContractMapper
        extends BaseMapper<CdpWarehouseConsumerAvailabilityContractDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_consumer_availability_contract
            (tenant_id, contract_key, consumer_type, consumer_ref, dataset_key, metric_key,
             required_mode, required_assets_json, gate_policy, warn_tolerance_minutes,
             status, owner_name, description)
            VALUES
            (#{row.tenantId}, #{row.contractKey}, #{row.consumerType}, #{row.consumerRef},
             #{row.datasetKey}, #{row.metricKey}, #{row.requiredMode}, #{row.requiredAssetsJson},
             #{row.gatePolicy}, #{row.warnToleranceMinutes}, #{row.status},
             #{row.ownerName}, #{row.description})
            ON DUPLICATE KEY UPDATE
                consumer_type = VALUES(consumer_type),
                consumer_ref = VALUES(consumer_ref),
                dataset_key = VALUES(dataset_key),
                metric_key = VALUES(metric_key),
                required_mode = VALUES(required_mode),
                required_assets_json = VALUES(required_assets_json),
                gate_policy = VALUES(gate_policy),
                warn_tolerance_minutes = VALUES(warn_tolerance_minutes),
                status = VALUES(status),
                owner_name = VALUES(owner_name),
                description = VALUES(description),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseConsumerAvailabilityContractDO row);

    @Update("""
            UPDATE cdp_warehouse_consumer_availability_contract
            SET last_evaluated_at = #{evaluatedAt},
                last_status = #{status},
                last_message = #{message},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND contract_key = #{contractKey}
            """)
    int updateEvaluation(@Param("tenantId") Long tenantId,
                         @Param("contractKey") String contractKey,
                         @Param("evaluatedAt") LocalDateTime evaluatedAt,
                         @Param("status") String status,
                         @Param("message") String message);
}
