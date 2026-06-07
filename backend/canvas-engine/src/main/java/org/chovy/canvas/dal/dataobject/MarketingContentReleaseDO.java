package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_content_release")
public class MarketingContentReleaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String releaseKey;
    private String sourceType;
    private String sourceKey;
    private Integer sourceVersion;
    private String channel;
    private String status;
    private String snapshotJson;
    private String assetRefsJson;
    private String checksumSha256;
    private String rollbackReason;
    private String createdBy;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
