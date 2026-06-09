package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingMonitorPollRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_poll_run")
public class MarketingMonitorPollRunDO {

    /** 营销监控轮询运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控轮询运行来源 ID */
    private Long sourceId;

    /** 营销监控轮询运行来源业务键 */
    private String sourceKey;

    /** 营销监控轮询运行来源类型 */
    private String sourceType;

    /** 营销监控轮询运行当前状态 */
    private String status;

    /** 营销监控轮询运行请求来源 */
    private LocalDateTime requestedFrom;

    /** 营销监控轮询运行请求截止 */
    private LocalDateTime requestedUntil;

    /** 营销监控轮询运行游标之前 */
    private String cursorBefore;

    /** 营销监控轮询运行游标之后 */
    private String cursorAfter;

    /** 营销监控轮询运行事项数量 */
    private Integer itemCount;

    /** 营销监控轮询运行新增数量 */
    private Integer insertedCount;

    /** 营销监控轮询运行重复数量 */
    private Integer duplicateCount;

    /** 营销监控轮询运行告警数量 */
    private Integer alertCount;

    /** 营销监控轮询运行错误信息 */
    private String errorMessage;

    /** 营销监控轮询运行扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控轮询运行创建人 */
    private String createdBy;

    /** 营销监控轮询运行开始时间 */
    private LocalDateTime startedAt;

    /** 营销监控轮询运行结束时间 */
    private LocalDateTime finishedAt;

    /** 营销监控轮询运行创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控轮询运行最后更新时间 */
    private LocalDateTime updatedAt;
}
