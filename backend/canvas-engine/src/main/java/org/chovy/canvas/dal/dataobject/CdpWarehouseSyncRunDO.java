package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_sync_run")
public class CdpWarehouseSyncRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String jobType;

    private String sourceTable;

    private Long sourceStartId;

    private Long sourceEndId;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private String status;

    private Long loadedRows;

    private Long failedRows;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String createdBy;
}
