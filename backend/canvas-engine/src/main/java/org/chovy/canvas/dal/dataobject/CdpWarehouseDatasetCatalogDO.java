package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_dataset_catalog")
public class CdpWarehouseDatasetCatalogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String datasetKey;

    private String layer;

    private String physicalName;

    private String displayName;

    private String subjectArea;

    private String sourceSystem;

    private String ownerName;

    private String description;

    private Integer freshnessSlaMinutes;

    private String piiLevel;

    private String status;

    private String schemaJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
