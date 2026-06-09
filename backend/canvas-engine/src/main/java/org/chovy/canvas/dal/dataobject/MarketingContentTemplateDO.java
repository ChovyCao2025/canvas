package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingContentTemplateDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_content_template")
public class MarketingContentTemplateDO {
    /** 营销内容模板主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 营销内容模板模板业务键 */
    private String templateKey;
    /** 营销内容模板展示名称 */
    private String displayName;
    /** 营销内容模板触达渠道 */
    private String channel;
    /** 营销内容模板主体 */
    private String subject;
    /** 营销内容模板正文 */
    private String body;
    /** 营销内容模板设计明细 JSON */
    private String designJson;
    /** 营销内容模板资产引用 JSON */
    private String assetRefsJson;
    /** 营销内容模板变量明细 JSON */
    private String variablesJson;
    /** 营销内容模板当前状态 */
    private String status;
    /** 营销内容模板评审备注 */
    private String reviewNotes;
    /** 营销内容模板创建人 */
    private String createdBy;
    /** 营销内容模板创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 营销内容模板最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 返回兼容模板变更记录字段的评审备注。
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
