package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SearchMarketingSyncRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_sync_run")
public class SearchMarketingSyncRunDO {

    /** 搜索营销同步运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销同步运行来源 ID */
    private Long sourceId;

    /** 搜索营销同步运行运行类型 */
    private String runType;

    /** 搜索营销同步运行服务商 */
    private String provider;

    /** 搜索营销同步运行触达渠道 */
    private String channel;

    /** 搜索营销同步运行幂等键 */
    private String idempotencyKey;

    /** 搜索营销同步运行窗口开始时间 */
    private LocalDate windowStart;

    /** 搜索营销同步运行窗口结束时间 */
    private LocalDate windowEnd;

    /** 搜索营销同步运行游标值 */
    private String cursorValue;

    /** 搜索营销同步运行当前状态 */
    private String status;

    /** 搜索营销同步运行可重试 */
    private Integer retryable;

    /** 搜索营销同步运行请求处理数量 */
    private Long requestedCount;

    /** 搜索营销同步运行成功数量 */
    private Long successCount;

    /** 搜索营销同步运行处理失败数量 */
    private Long failedCount;

    /** 关联的服务商请求 ID */
    private String providerRequestId;

    /** 搜索营销同步运行错误码 */
    private String errorCode;

    /** 搜索营销同步运行错误信息 */
    private String errorMessage;

    /** 搜索营销同步运行证据明细 JSON */
    private String evidenceJson;

    /** 搜索营销同步运行创建人 */
    private String createdBy;

    /** 搜索营销同步运行开始时间 */
    private LocalDateTime startedAt;

    /** 搜索营销同步运行结束时间 */
    private LocalDateTime finishedAt;

    /** 搜索营销同步运行最后更新时间 */
    private LocalDateTime updatedAt;
}
