package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingMonitorAlertDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_alert")
public class MarketingMonitorAlertDO {

    /** 营销监控告警主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控告警告警类型 */
    private String alertType;

    /** 营销监控告警严重级别 */
    private String severity;

    /** 营销监控告警当前状态 */
    private String status;

    /** 营销监控告警范围业务键 */
    private String scopeKey;

    /** 营销监控告警去重业务键 */
    private String dedupeKey;

    /** 营销监控告警标题 */
    private String title;

    /** 营销监控告警原因说明 */
    private String reason;

    /** 营销监控告警事项数量 */
    private Integer itemCount;

    /** 营销监控告警窗口开始时间 */
    private LocalDateTime windowStart;

    /** 营销监控告警窗口结束时间 */
    private LocalDateTime windowEnd;

    /** 营销监控告警扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控告警创建人 */
    private String createdBy;

    /** 营销监控告警解决人 */
    private String resolvedBy;

    /** 营销监控告警解决时间 */
    private LocalDateTime resolvedAt;

    /** 营销监控告警创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控告警最后更新时间 */
    private LocalDateTime updatedAt;
}
