package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_metric_change_review")
public class CdpWarehouseMetricChangeReviewDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String datasetKey;

    private String metricKey;

    private String changeType;

    private String currentSnapshotJson;

    private String proposedSnapshotJson;

    private String impactSummaryJson;

    private String riskLevel;

    private String status;

    private String requestedBy;

    private String requestReason;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewNote;

    private LocalDateTime appliedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
