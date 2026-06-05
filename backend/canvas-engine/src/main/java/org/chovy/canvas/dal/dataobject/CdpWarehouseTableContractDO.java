package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_table_contract")
public class CdpWarehouseTableContractDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String tableKey;

    private String datasetKey;

    private String layer;

    private String physicalName;

    private String engineType;

    private String ddlAssetPath;

    private String partitionColumn;

    private String partitionGranularity;

    private Integer retentionDays;

    private Integer replicaCount;

    private Integer bucketCount;

    private String distributionColumns;

    private String storagePolicy;

    private String lifecycleStatus;

    private String ownerName;

    private String description;

    private String expectedPropertiesJson;

    private LocalDateTime lastInspectedAt;

    private String lastInspectionStatus;

    private String lastInspectionMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
