package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthActivityDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_activity")
public class GrowthActivityDO {

    /** 增长活动主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 增长活动活动业务键 */
    private String activityKey;

    /** 增长活动活动名称 */
    private String activityName;

    /** 增长活动活动类型 */
    private String activityType;

    /** 增长活动当前状态 */
    private String status;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 增长活动目标 */
    private String objective;

    /** 增长活动负责团队 */
    private String ownerTeam;

    /** 增长活动开始时间 */
    private LocalDateTime startAt;

    /** 增长活动结束时间 */
    private LocalDateTime endAt;

    /** 增长活动渠道范围 */
    private String channelScope;

    /** 增长活动人群引用明细 JSON */
    private String audienceRefsJson;

    /** 增长活动风险策略引用 */
    private String riskPolicyRef;

    /** 增长活动实验引用 */
    private String experimentRef;

    /** 增长活动仪表板引用 */
    private String dashboardRef;

    /** 增长活动扩展元数据 JSON */
    private String metadataJson;

    /** 增长活动创建人 */
    private String createdBy;

    /** 增长活动最后更新人 */
    private String updatedBy;

    /** 增长活动创建时间 */
    private LocalDateTime createdAt;

    /** 增长活动最后更新时间 */
    private LocalDateTime updatedAt;
}
