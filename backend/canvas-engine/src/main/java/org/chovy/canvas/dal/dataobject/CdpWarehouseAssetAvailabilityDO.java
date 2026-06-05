package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_asset_availability")
public class CdpWarehouseAssetAvailabilityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String assetType;

    private String assetKey;

    private String availabilityMode;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private LocalDateTime availableUntil;

    private String status;

    private String evidenceSource;

    private String evidenceRef;

    private String reason;

    private LocalDateTime observedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
