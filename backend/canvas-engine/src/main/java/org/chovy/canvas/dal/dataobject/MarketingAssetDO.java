package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_asset")
public class MarketingAssetDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String assetKey;
    private String name;
    private String assetType;
    private String mimeType;
    private String storageUrl;
    private Long folderId;
    private Long sizeBytes;
    private String checksumSha256;
    private String thumbnailUrl;
    private String posterUrl;
    private Integer width;
    private Integer height;
    private Long durationMs;
    private String transcodeStatus;
    private String tagsJson;
    private String metadataJson;
    private String status;
    private String reviewNotes;
    private Integer referenceCount;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public String getDisplayName() {
        return name;
    }

    public void setDisplayName(String displayName) {
        this.name = displayName;
    }

    public String getPublicUrl() {
        return storageUrl;
    }

    public void setPublicUrl(String publicUrl) {
        if (publicUrl != null && !publicUrl.isBlank()) {
            this.storageUrl = publicUrl;
        }
    }

    public Long getFileSizeBytes() {
        return sizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.sizeBytes = fileSizeBytes;
    }

    public String getChecksum() {
        return checksumSha256;
    }

    public void setChecksum(String checksum) {
        this.checksumSha256 = checksum;
    }

    public String getReviewNote() {
        return reviewNotes;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNotes = reviewNote;
    }
}
