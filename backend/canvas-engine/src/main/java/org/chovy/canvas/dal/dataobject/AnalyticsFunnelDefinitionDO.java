package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AnalyticsFunnelDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("analytics_funnel_definition")
public class AnalyticsFunnelDefinitionDO {

    /** 分析漏斗定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析漏斗定义漏斗业务键 */
    private String funnelKey;

    /** 分析漏斗定义版本号 */
    private Integer version;

    /** 分析漏斗定义名称 */
    private String name;

    /** 分析漏斗定义步骤明细 JSON */
    private String stepsJson;

    /** 分析漏斗定义是否启用 */
    private Boolean enabled;

    /** 分析漏斗定义创建人 */
    private String createdBy;

    /** 分析漏斗定义创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
