package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentEntryDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_entry")
public class MarketingContentEntryDO {
    /** 营销内容内容主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容内容内容业务键 */
    private String entryKey;
    /** 营销内容内容内容类型 */
    private String contentType;
    /** 营销内容内容标题 */
    private String title;
    /** 营销内容内容短链 */
    private String slug;
    /** 营销内容内容语言环境 */
    private String locale;
    /** 营销内容内容摘要 */
    private String summary;
    /** 营销内容内容正文明细 JSON */
    private String bodyJson;
    /** 营销内容内容SEO明细 JSON */
    private String seoJson;
    /** 营销内容内容资产引用 JSON */
    private String assetRefsJson;
    /** 营销内容内容当前状态 */
    private String status;
    /** 营销内容内容发布时间 */
    private LocalDateTime publishedAt;
    /** 营销内容内容创建人 */
    private String createdBy;
    /** 营销内容内容最后更新人 */
    private String updatedBy;
    /** 营销内容内容创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销内容内容最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
