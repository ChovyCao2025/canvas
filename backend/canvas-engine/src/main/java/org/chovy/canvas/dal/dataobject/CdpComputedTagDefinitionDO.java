package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpComputedTagDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_computed_tag_definition")
public class CdpComputedTagDefinitionDO {
    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";

    /** CDP计算标签定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP计算标签定义标签编码 */
    private String tagCode;
    /** CDP计算标签定义展示名称 */
    private String displayName;
    /** CDP计算标签定义值类型 */
    private String valueType;
    /** CDP计算标签定义计算类型 */
    private String computeType;
    /** CDP计算标签定义表达式明细 JSON */
    private String expressionJson;
    /** CDP计算标签定义刷新模式 */
    private String refreshMode;
    /** CDP计算标签定义当前状态 */
    private String status;
    /** CDP计算标签定义创建人 */
    private String createdBy;
    /** CDP计算标签定义创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** CDP计算标签定义最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
