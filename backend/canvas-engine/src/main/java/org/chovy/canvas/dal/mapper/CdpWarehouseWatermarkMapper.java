package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;

/**
 * CdpWarehouseWatermarkMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    int upsert(@Param("row") CdpWarehouseWatermarkDO row);
}
