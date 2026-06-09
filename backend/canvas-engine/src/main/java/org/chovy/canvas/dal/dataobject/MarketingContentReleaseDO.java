package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentReleaseDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_release")
public class MarketingContentReleaseDO {
    /** 营销内容发布主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容发布发布业务键 */
    private String releaseKey;
    /** 营销内容发布来源类型 */
    private String sourceType;
    /** 营销内容发布来源业务键 */
    private String sourceKey;
    /** 营销内容发布来源版本 */
    private Integer sourceVersion;
    /** 营销内容发布触达渠道 */
    private String channel;
    /** 营销内容发布当前状态 */
    private String status;
    /** 营销内容发布快照明细 JSON */
    private String snapshotJson;
    /** 营销内容发布资产引用 JSON */
    private String assetRefsJson;
    /** 营销内容发布校验和SHA-256 */
    private String checksumSha256;
    /** 营销内容发布回滚原因 */
    private String rollbackReason;
    /** 营销内容发布创建人 */
    private String createdBy;
    /** 营销内容发布发布时间 */
    private LocalDateTime publishedAt;
    /** 营销内容发布创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销内容发布最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
