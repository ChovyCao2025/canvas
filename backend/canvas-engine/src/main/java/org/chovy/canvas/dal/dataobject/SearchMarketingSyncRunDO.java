package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_marketing_sync_run")
public class SearchMarketingSyncRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private String runType;

    private String provider;

    private String channel;

    private String idempotencyKey;

    private LocalDate windowStart;

    private LocalDate windowEnd;

    private String cursorValue;

    private String status;

    private Integer retryable;

    private Long requestedCount;

    private Long successCount;

    private Long failedCount;

    private String providerRequestId;

    private String errorCode;

    private String errorMessage;

    private String evidenceJson;

    private String createdBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime updatedAt;
}
