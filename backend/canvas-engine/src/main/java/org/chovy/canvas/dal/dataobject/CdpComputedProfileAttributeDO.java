package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpComputedProfileAttributeDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_computed_profile_attribute")
public class CdpComputedProfileAttributeDO {
    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";

    /** CDP计算画像属性主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP计算画像属性属性编码 */
    private String attrCode;
    /** CDP计算画像属性展示名称 */
    private String displayName;
    /** CDP计算画像属性值类型 */
    private String valueType;
    /** CDP计算画像属性计算类型 */
    private String computeType;
    /** CDP计算画像属性表达式明细 JSON */
    private String expressionJson;
    /** CDP计算画像属性刷新模式 */
    private String refreshMode;
    /** CDP计算画像属性当前状态 */
    private String status;
    /** CDP计算画像属性创建人 */
    private String createdBy;
    /** CDP计算画像属性创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** CDP计算画像属性最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
