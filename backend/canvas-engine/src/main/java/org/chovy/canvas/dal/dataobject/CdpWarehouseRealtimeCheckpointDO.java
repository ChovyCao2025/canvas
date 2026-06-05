package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_realtime_checkpoint")
public class CdpWarehouseRealtimeCheckpointDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String streamKey;

    private Long lastEventLogId;

    private String lastMessageId;

    private String lastEventCode;

    private LocalDateTime lastEventTime;

    private LocalDateTime lastReceivedAt;

    private LocalDateTime lastDeliveredAt;

    private String lastDeliverySource;

    private Long deliveredCount;

    private Long failureCount;

    private LocalDateTime lastFailureAt;

    private String lastFailureMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
