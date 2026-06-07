package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_asset_upload_intent")
public class MarketingAssetUploadIntentDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String intentKey;
    private String assetKey;
    private String assetType;
    private String provider;
    private String mimeType;
    private String fileName;
    private Long sizeBytes;
    private String uploadToken;
    private String uploadUrl;
    private String uploadParamsJson;
    private String status;
    private String providerAssetId;
    private String callbackJson;
    private String errorMessage;
    private LocalDateTime expiresAt;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
