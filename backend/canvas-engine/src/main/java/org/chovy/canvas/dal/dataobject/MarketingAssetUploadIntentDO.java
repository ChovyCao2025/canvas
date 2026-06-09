package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingAssetUploadIntentDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_asset_upload_intent")
public class MarketingAssetUploadIntentDO {
    /** 营销资产上传意图主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销资产上传意图意图业务键 */
    private String intentKey;
    /** 营销资产上传意图资产业务键 */
    private String assetKey;
    /** 营销资产上传意图资产类型 */
    private String assetType;
    /** 营销资产上传意图服务商 */
    private String provider;
    /** 营销资产上传意图MIME类型 */
    private String mimeType;
    /** 营销资产上传意图文件名 */
    private String fileName;
    /** 营销资产上传意图文件大小字节数 */
    private Long sizeBytes;
    /** 营销资产上传意图上传令牌 */
    private String uploadToken;
    /** 营销资产上传意图上传URL */
    private String uploadUrl;
    /** 营销资产上传意图上传参数明细 JSON */
    private String uploadParamsJson;
    /** 营销资产上传意图当前状态 */
    private String status;
    /** 关联的服务商资产 ID */
    private String providerAssetId;
    /** 营销资产上传意图回调明细 JSON */
    private String callbackJson;
    /** 营销资产上传意图错误信息 */
    private String errorMessage;
    /** 营销资产上传意图过期时间 */
    private LocalDateTime expiresAt;
    /** 营销资产上传意图创建人 */
    private String createdBy;
    /** 营销资产上传意图创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销资产上传意图最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
