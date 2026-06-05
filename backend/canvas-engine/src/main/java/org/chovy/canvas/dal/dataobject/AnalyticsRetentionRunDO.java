package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analytics_retention_run")
public class AnalyticsRetentionRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String recordKind;

    private String action;

    private Boolean dryRun;

    private Long scannedCount;

    private Long archivedCount;

    private Long deletedCount;

    private Long skippedCount;

    private Long failedCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
