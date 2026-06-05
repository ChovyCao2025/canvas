package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_stream_checkpoint")
public class CdpWarehouseStreamCheckpointDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String checkpointId;

    private String sourcePartition;

    private String sourceOffset;

    private String committedOffset;

    private LocalDateTime watermarkTime;

    private LocalDateTime checkpointTime;

    private Long lagMs;

    private Long rowCount;

    private String status;

    private String errorMessage;

    private String reportedBy;

    private String sourceSchemaVersion;

    private String sinkSchemaVersion;

    private String schemaStatus;

    private LocalDateTime createdAt;
}
