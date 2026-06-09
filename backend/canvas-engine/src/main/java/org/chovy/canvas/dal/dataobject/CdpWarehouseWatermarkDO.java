package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseWatermarkDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_watermark")
public class CdpWarehouseWatermarkDO {

    /** CDP数仓水位主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓水位任务名称 */
    private String jobName;

    /** CDP数仓水位水位类型 */
    private String watermarkType;

    /** CDP数仓水位水位值 */
    private String watermarkValue;

    /** CDP数仓水位水位时间 */
    private LocalDateTime watermarkTime;

    /** CDP数仓水位最后更新时间 */
    private LocalDateTime updatedAt;
}
