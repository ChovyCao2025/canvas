package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PaidMediaAudienceSyncRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("paid_media_audience_sync_run")
public class PaidMediaAudienceSyncRunDO {

    /** 付费媒体人群同步运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的目标端 ID */
    private Long destinationId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 付费媒体人群同步运行服务商 */
    private String provider;

    /** 付费媒体人群同步运行当前状态 */
    private String status;

    /** 付费媒体人群同步运行请求处理数量 */
    private Integer requestedCount;

    /** 付费媒体人群同步运行符合资格数量 */
    private Integer eligibleCount;

    /** 付费媒体人群同步运行已跳过数量 */
    private Integer skippedCount;

    /** 付费媒体人群同步运行处理失败数量 */
    private Integer failedCount;

    /** 关联的外部操作 ID */
    private String externalOperationId;

    /** 付费媒体人群同步运行错误信息 */
    private String errorMessage;

    /** 付费媒体人群同步运行扩展元数据 JSON */
    private String metadataJson;

    /** 付费媒体人群同步运行创建人 */
    private String createdBy;

    /** 付费媒体人群同步运行开始时间 */
    private LocalDateTime startedAt;

    /** 付费媒体人群同步运行结束时间 */
    private LocalDateTime finishedAt;

    /** 付费媒体人群同步运行创建时间 */
    private LocalDateTime createdAt;

    /** 付费媒体人群同步运行最后更新时间 */
    private LocalDateTime updatedAt;
}
