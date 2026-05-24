package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人群定义（audience_definition）。
 *
 * <p>用于描述可被 TAGGER 节点引用的人群圈选规则与计算策略。
 */
@Data
@TableName("audience_definition")
public class AudienceDefinitionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 人群名称。 */
    private String name;

    /** 人群描述（可选）。 */
    private String description;

    /** 规则表达式 JSON（由前端规则编辑器生成）。 */
    private String ruleJson;

    /** 规则引擎类型：AVIATOR | QL。 */
    private String engineType;

    /** 数据源类型：TAGGER_API | JDBC。 */
    private String dataSourceType;

    /** 数据源配置（JSON 字符串，可选）。 */
    private String dataSourceConfig;

    /** 计算策略：ONLINE | OFFLINE_BATCH | HYBRID。 */
    private String evaluationStrategy;

    /** 离线批量计算 cron（evaluationStrategy=OFFLINE_BATCH 时使用）。 */
    private String cronExpression;

    /** 启用状态：1=启用，0=禁用。 */
    private Integer enabled;

    /** 创建人。 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
