package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_lineage_edge")
public class CdpWarehouseLineageEdgeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String upstreamDatasetKey;

    private String downstreamDatasetKey;

    private String transformType;

    private String transformRef;

    private String dependencyType;

    private String description;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
