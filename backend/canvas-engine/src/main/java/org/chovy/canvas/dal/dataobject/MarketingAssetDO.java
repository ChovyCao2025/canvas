package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingAssetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_asset")
public class MarketingAssetDO {
    /** 营销资产主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销资产资产业务键 */
    private String assetKey;
    /** 营销资产名称 */
    private String name;
    /** 营销资产资产类型 */
    private String assetType;
    /** 营销资产MIME类型 */
    private String mimeType;
    /** 营销资产存储URL */
    private String storageUrl;
    /** 关联的文件夹 ID */
    private Long folderId;
    /** 营销资产文件大小字节数 */
    private Long sizeBytes;
    /** 营销资产校验和SHA-256 */
    private String checksumSha256;
    /** 营销资产缩略图URL */
    private String thumbnailUrl;
    /** 营销资产海报URL */
    private String posterUrl;
    /** 营销资产宽度 */
    private Integer width;
    /** 营销资产高度 */
    private Integer height;
    /** 营销资产耗时毫秒数 */
    private Long durationMs;
    /** 营销资产转码状态 */
    private String transcodeStatus;
    /** 营销资产标签列表 JSON */
    private String tagsJson;
    /** 营销资产扩展元数据 JSON */
    private String metadataJson;
    /** 营销资产当前状态 */
    private String status;
    /** 营销资产评审备注 */
    private String reviewNotes;
    /** 营销资产引用数量 */
    private Integer referenceCount;
    /** 营销资产创建人 */
    private String createdBy;
    /** 营销资产创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销资产最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 返回兼容前端展示字段的资产名称。
     *
     * @return 资产展示名称
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * setDisplayName 处理 dal.dataobject 场景的业务逻辑。
     * @param displayName 名称文本，用于展示或唯一性校验。
     */
    public void setDisplayName(String displayName) {
        this.name = displayName;
    }

    /**
     * getPublicUrl 查询 dal.dataobject 场景的业务数据。
     * @return 返回 get public url 生成的文本或业务键。
     */
    public String getPublicUrl() {
        return storageUrl;
    }

    /**
     * setPublicUrl 处理 dal.dataobject 场景的业务逻辑。
     * @param publicUrl public url 参数，用于 setPublicUrl 流程中的校验、计算或对象转换。
     */
    public void setPublicUrl(String publicUrl) {
        if (publicUrl != null && !publicUrl.isBlank()) {
            this.storageUrl = publicUrl;
        }
    }

    /**
     * getFileSizeBytes 查询 dal.dataobject 场景的业务数据。
     * @return 返回 get file size bytes 计算得到的数量、金额或指标值。
     */
    public Long getFileSizeBytes() {
        return sizeBytes;
    }

    /**
     * setFileSizeBytes 处理 dal.dataobject 场景的业务逻辑。
     * @param fileSizeBytes file size bytes 参数，用于 setFileSizeBytes 流程中的校验、计算或对象转换。
     */
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.sizeBytes = fileSizeBytes;
    }

    /**
     * getChecksum 查询 dal.dataobject 场景的业务数据。
     * @return 返回 get checksum 生成的文本或业务键。
     */
    public String getChecksum() {
        return checksumSha256;
    }

    /**
     * setChecksum 处理 dal.dataobject 场景的业务逻辑。
     * @param checksum checksum 参数，用于 setChecksum 流程中的校验、计算或对象转换。
     */
    public void setChecksum(String checksum) {
        this.checksumSha256 = checksum;
    }

    /**
     * getReviewNote 查询 dal.dataobject 场景的业务数据。
     * @return 返回 get review note 生成的文本或业务键。
     */
    public String getReviewNote() {
        return reviewNotes;
    }

    /**
     * setReviewNote 处理 dal.dataobject 场景的业务逻辑。
     * @param reviewNote review note 参数，用于 setReviewNote 流程中的校验、计算或对象转换。
     */
    public void setReviewNote(String reviewNote) {
        this.reviewNotes = reviewNote;
    }
}
