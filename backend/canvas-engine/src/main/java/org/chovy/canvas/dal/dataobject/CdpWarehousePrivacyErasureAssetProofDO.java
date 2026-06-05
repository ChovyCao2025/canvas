package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_privacy_erasure_asset_proof")
public class CdpWarehousePrivacyErasureAssetProofDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long requestId;

    private String requestKey;

    private String assetKey;

    private String assetLayer;

    private String actionType;

    private String status;

    private String plannedAction;

    private Long matchedCount;

    private Long affectedCount;

    private String proofMessage;

    private String errorMessage;

    private String executedBy;

    private LocalDateTime executedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
