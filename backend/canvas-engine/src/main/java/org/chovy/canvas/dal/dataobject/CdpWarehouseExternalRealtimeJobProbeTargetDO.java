package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_external_realtime_job_probe_target")
public class CdpWarehouseExternalRealtimeJobProbeTargetDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String jobKey;

    private String engineType;

    private String endpointUrl;

    private String authRef;

    private String externalJobId;

    private String connectorName;

    private String deploymentRef;

    private Integer enabled;

    private String ownerName;

    private Integer maxStalenessSeconds;

    private String configJson;

    private LocalDateTime lastProbedAt;

    private String lastProbeStatus;

    private String lastProbeMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
