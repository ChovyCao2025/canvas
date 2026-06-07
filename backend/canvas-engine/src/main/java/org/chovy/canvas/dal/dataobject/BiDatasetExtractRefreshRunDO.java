package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_dataset_extract_refresh_run")
public class BiDatasetExtractRefreshRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String datasetKey;
    private String status;
    private Long rowCount;
    private Long durationMs;
    private String materializedTable;
    private String retentionStatus;
    private String requestedBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime droppedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
