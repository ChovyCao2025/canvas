package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_enterprise_olap_evidence_collection_run")
public class CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO {

    /** CDP数仓企业OLAP证据采集运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓企业OLAP证据采集运行触发类型 */
    private String triggerType;

    /** CDP数仓企业OLAP证据采集运行当前状态 */
    private String status;

    /** CDP数仓企业OLAP证据采集运行开始时间 */
    private LocalDateTime startedAt;

    /** CDP数仓企业OLAP证据采集运行结束时间 */
    private LocalDateTime finishedAt;

    /** CDP数仓企业OLAP证据采集运行证据数量 */
    private Integer evidenceCount;

    /** CDP数仓企业OLAP证据采集运行通过数量 */
    private Integer passCount;

    /** CDP数仓企业OLAP证据采集运行预警数量 */
    private Integer warnCount;

    /** CDP数仓企业OLAP证据采集运行失败数量 */
    private Integer failCount;

    /** CDP数仓企业OLAP证据采集运行原因说明 */
    private String reason;

    /** CDP数仓企业OLAP证据采集运行创建人 */
    private String createdBy;

    /** CDP数仓企业OLAP证据采集运行创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓企业OLAP证据采集运行最后更新时间 */
    private LocalDateTime updatedAt;
}
