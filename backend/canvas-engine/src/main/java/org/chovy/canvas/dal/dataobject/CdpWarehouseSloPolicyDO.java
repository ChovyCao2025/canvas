package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_slo_policy")
public class CdpWarehouseSloPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String policyKey;

    private String displayName;

    private Integer offlineWarnRunGapMinutes;

    private Integer offlineFailRunGapMinutes;

    private Integer offlineWarnWatermarkLagMinutes;

    private Integer offlineFailWatermarkLagMinutes;

    private Integer audienceWarnRunGapMinutes;

    private Integer audienceFailRunGapMinutes;

    private String status;

    private String ownerName;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
