package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_synthetic_data_path_probe_run")
public class CdpWarehouseSyntheticDataPathProbeRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String probeKey;

    private String sourceMode;

    private String messageId;

    private String eventCode;

    private String userId;

    private Integer strictMode;

    private String status;

    private String sourceStatus;

    private String sinkStatus;

    private String odsStatus;

    private Long odsRowCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String errorMessage;

    private String evidenceJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
