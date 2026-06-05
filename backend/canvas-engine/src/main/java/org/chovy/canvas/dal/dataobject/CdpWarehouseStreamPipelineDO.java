package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_stream_pipeline")
public class CdpWarehouseStreamPipelineDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String displayName;

    private String sourceType;

    private String sourceRef;

    private String sourceTopic;

    private String consumerGroup;

    private String processorType;

    private String sinkType;

    private String sinkRef;

    private String deliverySemantics;

    private Integer checkpointIntervalSeconds;

    private Long maxLagMs;

    private Integer maxCheckpointAgeSeconds;

    private String lifecycleStatus;

    private String ownerName;

    private String configJson;

    private String lastCheckpointId;

    private String lastSourceOffset;

    private String lastCommittedOffset;

    private LocalDateTime lastWatermarkTime;

    private LocalDateTime lastCheckpointAt;

    private Long lastLagMs;

    private String lastRuntimeStatus;

    private String lastStatusMessage;

    private String lastReportedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
