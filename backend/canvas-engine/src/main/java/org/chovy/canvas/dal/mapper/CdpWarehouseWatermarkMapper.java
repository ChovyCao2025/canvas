package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;

@Mapper
public interface CdpWarehouseWatermarkMapper extends BaseMapper<CdpWarehouseWatermarkDO> {

    @Insert("""
            INSERT INTO cdp_warehouse_watermark
            (tenant_id, job_name, watermark_type, watermark_value, watermark_time)
            VALUES
            (#{row.tenantId}, #{row.jobName}, #{row.watermarkType}, #{row.watermarkValue}, #{row.watermarkTime})
            ON DUPLICATE KEY UPDATE
                watermark_value = VALUES(watermark_value),
                watermark_time = VALUES(watermark_time),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("row") CdpWarehouseWatermarkDO row);
}
