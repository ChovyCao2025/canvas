package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseTableContractDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_table_contract")
public class CdpWarehouseTableContractDO {

    /** CDP数仓表契约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓表契约表业务键 */
    private String tableKey;

    /** CDP数仓表契约数据集业务键 */
    private String datasetKey;

    /** CDP数仓表契约流量层 */
    private String layer;

    /** CDP数仓表契约物理名称 */
    private String physicalName;

    /** CDP数仓表契约引擎类型 */
    private String engineType;

    /** CDP数仓表契约DDL资产路径 */
    private String ddlAssetPath;

    /** CDP数仓表契约分区列 */
    private String partitionColumn;

    /** CDP数仓表契约分区粒度 */
    private String partitionGranularity;

    /** CDP数仓表契约保留天数 */
    private Integer retentionDays;

    /** CDP数仓表契约副本数量 */
    private Integer replicaCount;

    /** CDP数仓表契约分桶数量 */
    private Integer bucketCount;

    /** CDP数仓表契约分布列 */
    private String distributionColumns;

    /** CDP数仓表契约存储策略 */
    private String storagePolicy;

    /** CDP数仓表契约生命周期状态 */
    private String lifecycleStatus;

    /** CDP数仓表契约负责人姓名 */
    private String ownerName;

    /** CDP数仓表契约说明 */
    private String description;

    /** CDP数仓表契约预期属性明细 JSON */
    private String expectedPropertiesJson;

    /** CDP数仓表契约最近巡检时间 */
    private LocalDateTime lastInspectedAt;

    /** CDP数仓表契约最近巡检状态 */
    private String lastInspectionStatus;

    /** CDP数仓表契约最近巡检消息 */
    private String lastInspectionMessage;

    /** CDP数仓表契约创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓表契约最后更新时间 */
    private LocalDateTime updatedAt;
}
