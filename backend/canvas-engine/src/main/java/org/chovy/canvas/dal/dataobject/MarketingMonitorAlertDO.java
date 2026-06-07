package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_alert")
public class MarketingMonitorAlertDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String alertType;

    private String severity;

    private String status;

    private String scopeKey;

    private String dedupeKey;

    private String title;

    private String reason;

    private Integer itemCount;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private String metadataJson;

    private String createdBy;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
