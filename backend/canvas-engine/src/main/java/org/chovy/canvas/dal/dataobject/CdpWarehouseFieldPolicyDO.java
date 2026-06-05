package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_field_policy")
public class CdpWarehouseFieldPolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String datasetKey;

    private String fieldKey;

    private String physicalName;

    private String columnName;

    private String valueType;

    private String semanticType;

    private String piiLevel;

    private String accessPolicy;

    private String minRole;

    private String allowedUsages;

    private String maskStrategy;

    private String lifecycleStatus;

    private String ownerName;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
