package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_stream_job_action")
public class CdpWarehouseStreamJobActionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String jobKey;

    private String action;

    private String status;

    private String requestedBy;

    private String reason;

    private LocalDateTime requestedAt;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime completedAt;

    private String resultMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
