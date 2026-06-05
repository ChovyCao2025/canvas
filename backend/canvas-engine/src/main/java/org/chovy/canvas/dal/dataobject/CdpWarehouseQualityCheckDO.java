package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_quality_check")
public class CdpWarehouseQualityCheckDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String checkType;

    private String status;

    private Long sourceCount;

    private Long warehouseCount;

    private Long diffCount;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private Long thresholdValue;

    private String detailsJson;

    private LocalDateTime checkedAt;

    private String createdBy;
}
