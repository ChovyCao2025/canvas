package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_watermark")
public class CdpWarehouseWatermarkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String jobName;

    private String watermarkType;

    private String watermarkValue;

    private LocalDateTime watermarkTime;

    private LocalDateTime updatedAt;
}
