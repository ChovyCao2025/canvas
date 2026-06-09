package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentTemplateVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_template_version")
public class MarketingContentTemplateVersionDO {
    /** 营销内容模板版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容模板版本模板业务键 */
    private String templateKey;
    /** 营销内容模板版本版本编号 */
    private Integer versionNo;
    /** 营销内容模板版本展示名称 */
    private String displayName;
    /** 营销内容模板版本触达渠道 */
    private String channel;
    /** 营销内容模板版本主体 */
    private String subject;
    /** 营销内容模板版本正文 */
    private String body;
    /** 营销内容模板版本设计明细 JSON */
    private String designJson;
    /** 营销内容模板版本资产引用 JSON */
    private String assetRefsJson;
    /** 营销内容模板版本变量明细 JSON */
    private String variablesJson;
    /** 营销内容模板版本当前状态 */
    private String status;
    /** 营销内容模板版本评审备注 */
    private String reviewNotes;
    /** 营销内容模板版本创建人 */
    private String createdBy;
    /** 营销内容模板版本创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 返回兼容模板版本变更记录字段的评审备注。
     *
     * @return 变更说明
     */
    public String getChangeNote() {
        return reviewNotes;
    }

    /**
     * setChangeNote 处理 dal.dataobject 场景的业务逻辑。
     * @param changeNote change note 参数，用于 setChangeNote 流程中的校验、计算或对象转换。
     */
    public void setChangeNote(String changeNote) {
        this.reviewNotes = changeNote;
    }
}
