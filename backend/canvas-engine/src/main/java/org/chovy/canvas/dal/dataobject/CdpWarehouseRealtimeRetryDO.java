package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_realtime_retry")
public class CdpWarehouseRealtimeRetryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long eventLogId;

    private String messageId;

    private String eventCode;

    private String status;

    private Integer attemptCount;

    private String firstError;

    private String lastError;

    private LocalDateTime nextRetryAt;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime finishedAt;
}
