package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AnalyticsRetentionPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("analytics_retention_policy")
public class AnalyticsRetentionPolicyDO {

    /** 分析留存策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析留存策略记录类型 */
    private String recordKind;

    /** 分析留存策略保留天数 */
    private Integer retentionDays;

    /** 分析留存策略动作 */
    private String action;

    /** 分析留存策略最大批次大小 */
    private Integer maxBatchSize;

    /** 分析留存策略法务保留行为 */
    private String legalHoldBehavior;

    /** 分析留存策略是否启用 */
    private Boolean enabled;

    /** 分析留存策略最后更新人 */
    private String updatedBy;

    /** 分析留存策略最后更新时间 */
    private LocalDateTime updatedAt;
}
