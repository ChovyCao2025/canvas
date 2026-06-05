package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSloPolicyDO;

@Mapper
public interface CdpWarehouseSloPolicyMapper extends BaseMapper<CdpWarehouseSloPolicyDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_slo_policy
            (tenant_id, policy_key, display_name,
             offline_warn_run_gap_minutes, offline_fail_run_gap_minutes,
             offline_warn_watermark_lag_minutes, offline_fail_watermark_lag_minutes,
             audience_warn_run_gap_minutes, audience_fail_run_gap_minutes,
             status, owner_name, description)
            VALUES
            (#{row.tenantId}, #{row.policyKey}, #{row.displayName},
             #{row.offlineWarnRunGapMinutes}, #{row.offlineFailRunGapMinutes},
             #{row.offlineWarnWatermarkLagMinutes}, #{row.offlineFailWatermarkLagMinutes},
             #{row.audienceWarnRunGapMinutes}, #{row.audienceFailRunGapMinutes},
             #{row.status}, #{row.ownerName}, #{row.description})
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                offline_warn_run_gap_minutes = VALUES(offline_warn_run_gap_minutes),
                offline_fail_run_gap_minutes = VALUES(offline_fail_run_gap_minutes),
                offline_warn_watermark_lag_minutes = VALUES(offline_warn_watermark_lag_minutes),
                offline_fail_watermark_lag_minutes = VALUES(offline_fail_watermark_lag_minutes),
                audience_warn_run_gap_minutes = VALUES(audience_warn_run_gap_minutes),
                audience_fail_run_gap_minutes = VALUES(audience_fail_run_gap_minutes),
                status = VALUES(status),
                owner_name = VALUES(owner_name),
                description = VALUES(description),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseSloPolicyDO row);
}
