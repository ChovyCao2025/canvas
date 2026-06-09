package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthActivityRuleSetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_activity_rule_set")
public class GrowthActivityRuleSetDO {

    /** 增长活动规则集合主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 增长活动规则集合规则集合业务键 */
    private String ruleSetKey;

    /** 增长活动规则集合规则集合类型 */
    private String ruleSetType;

    /** 增长活动规则集合当前状态 */
    private String status;

    /** 增长活动规则集合规则配置 JSON */
    private String ruleJson;

    /** 增长活动规则集合创建人 */
    private String createdBy;

    /** 增长活动规则集合最后更新人 */
    private String updatedBy;

    /** 增长活动规则集合创建时间 */
    private LocalDateTime createdAt;

    /** 增长活动规则集合最后更新时间 */
    private LocalDateTime updatedAt;
}
