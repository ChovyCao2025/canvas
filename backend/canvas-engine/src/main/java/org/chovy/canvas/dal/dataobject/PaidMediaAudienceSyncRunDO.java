package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paid_media_audience_sync_run")
public class PaidMediaAudienceSyncRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long destinationId;

    private Long audienceId;

    private String provider;

    private String status;

    private Integer requestedCount;

    private Integer eligibleCount;

    private Integer skippedCount;

    private Integer failedCount;

    private String externalOperationId;

    private String errorMessage;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
