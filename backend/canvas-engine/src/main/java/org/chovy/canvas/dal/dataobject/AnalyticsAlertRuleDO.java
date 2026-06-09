package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AnalyticsAlertRuleDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("analytics_alert_rule")
public class AnalyticsAlertRuleDO {

    /** 分析告警规则主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析告警规则规则业务键 */
    private String ruleKey;

    /** 分析告警规则名称 */
    private String name;

    /** 分析告警规则阈值明细 JSON */
    private String thresholdJson;

    /** 分析告警规则是否启用 */
    private Boolean enabled;

    /** 分析告警规则创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
