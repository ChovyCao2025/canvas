package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_stream_job_instance")
public class CdpWarehouseStreamJobInstanceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String jobKey;

    private String engineType;

    private String engineJobId;

    private String deploymentRef;

    private String runtimeStatus;

    private String desiredStatus;

    private LocalDateTime lastHeartbeatAt;

    private String heartbeatPayloadJson;

    private String lastErrorMessage;

    private String ownerName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
