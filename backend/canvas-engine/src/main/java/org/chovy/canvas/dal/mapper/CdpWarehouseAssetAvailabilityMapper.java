package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseAssetAvailabilityDO;

/**
 * CdpWarehouseAssetAvailabilityMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseAssetAvailabilityMapper extends BaseMapper<CdpWarehouseAssetAvailabilityDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_asset_availability
            (tenant_id, asset_type, asset_key, availability_mode, window_start, window_end,
             available_until, status, evidence_source, evidence_ref, reason, observed_at)
            VALUES
            (#{row.tenantId}, #{row.assetType}, #{row.assetKey}, #{row.availabilityMode},
             #{row.windowStart}, #{row.windowEnd}, #{row.availableUntil}, #{row.status},
             #{row.evidenceSource}, #{row.evidenceRef}, #{row.reason}, #{row.observedAt})
            ON DUPLICATE KEY UPDATE
                window_start = VALUES(window_start),
                window_end = VALUES(window_end),
                available_until = VALUES(available_until),
                status = VALUES(status),
                evidence_ref = VALUES(evidence_ref),
                reason = VALUES(reason),
                observed_at = VALUES(observed_at),
                updated_at = CURRENT_TIMESTAMP
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseAssetAvailabilityDO row);
}
