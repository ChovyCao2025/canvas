package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingFormDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_form_definition")
public class MarketingFormDefinitionDO {

    /** 营销表单定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 营销表单定义公开业务键 */
    private String publicKey;

    /** 营销表单定义名称 */
    private String name;

    /** 营销表单定义说明 */
    private String description;

    /** 营销表单定义当前状态 */
    private String status;

    /** 营销表单定义字段结构明细 JSON */
    private String fieldSchemaJson;

    /** 营销表单定义提交动作明细 JSON */
    private String submitActionJson;

    /** 营销表单定义成功消息 */
    private String successMessage;

    /** 营销表单定义创建人 */
    private String createdBy;

    /** 营销表单定义创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 营销表单定义最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
