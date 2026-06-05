package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_incident")
public class CdpWarehouseIncidentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String incidentKey;

    private String sourceType;

    private Long sourceId;

    private String severity;

    private String status;

    private String title;

    private String description;

    private Long occurrenceCount;

    private LocalDateTime firstSeenAt;

    private LocalDateTime lastSeenAt;

    private String acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
