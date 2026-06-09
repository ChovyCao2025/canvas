package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentEntryVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_entry_version")
public class MarketingContentEntryVersionDO {
    /** 营销内容内容版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容内容版本内容业务键 */
    private String entryKey;
    /** 营销内容内容版本版本编号 */
    private Integer versionNo;
    /** 营销内容内容版本内容类型 */
    private String contentType;
    /** 营销内容内容版本标题 */
    private String title;
    /** 营销内容内容版本短链 */
    private String slug;
    /** 营销内容内容版本语言环境 */
    private String locale;
    /** 营销内容内容版本摘要 */
    private String summary;
    /** 营销内容内容版本正文明细 JSON */
    private String bodyJson;
    /** 营销内容内容版本SEO明细 JSON */
    private String seoJson;
    /** 营销内容内容版本资产引用 JSON */
    private String assetRefsJson;
    /** 营销内容内容版本当前状态 */
    private String status;
    /** 营销内容内容版本创建人 */
    private String createdBy;
    /** 营销内容内容版本创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
