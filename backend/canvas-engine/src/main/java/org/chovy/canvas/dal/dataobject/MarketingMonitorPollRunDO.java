package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_poll_run")
public class MarketingMonitorPollRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private String sourceKey;

    private String sourceType;

    private String status;

    private LocalDateTime requestedFrom;

    private LocalDateTime requestedUntil;

    private String cursorBefore;

    private String cursorAfter;

    private Integer itemCount;

    private Integer insertedCount;

    private Integer duplicateCount;

    private Integer alertCount;

    private String errorMessage;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
