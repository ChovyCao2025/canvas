package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseMetricChangeReviewDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_metric_change_review")
public class CdpWarehouseMetricChangeReviewDO {

    /** CDP数仓指标变更评审主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓指标变更评审数据集业务键 */
    private String datasetKey;

    /** CDP数仓指标变更评审指标标识 */
    private String metricKey;

    /** CDP数仓指标变更评审变更类型 */
    private String changeType;

    /** CDP数仓指标变更评审当前快照明细 JSON */
    private String currentSnapshotJson;

    /** CDP数仓指标变更评审提议快照明细 JSON */
    private String proposedSnapshotJson;

    /** CDP数仓指标变更评审影响摘要明细 JSON */
    private String impactSummaryJson;

    /** CDP数仓指标变更评审风险级别 */
    private String riskLevel;

    /** CDP数仓指标变更评审当前状态 */
    private String status;

    /** CDP数仓指标变更评审请求人 */
    private String requestedBy;

    /** CDP数仓指标变更评审请求原因 */
    private String requestReason;

    /** CDP数仓指标变更评审审核人 */
    private String reviewedBy;

    /** CDP数仓指标变更评审审核时间 */
    private LocalDateTime reviewedAt;

    /** CDP数仓指标变更评审评审备注 */
    private String reviewNote;

    /** CDP数仓指标变更评审应用时间 */
    private LocalDateTime appliedAt;

    /** CDP数仓指标变更评审创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓指标变更评审最后更新时间 */
    private LocalDateTime updatedAt;
}
