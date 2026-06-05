package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_table_inspection")
public class CdpWarehouseTableInspectionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String tableKey;

    private String physicalName;

    private String status;

    private Integer checkedItems;

    private Integer violationCount;

    private String message;

    private String violationsJson;

    private String ddlAssetPath;

    private String inspectedBy;

    private LocalDateTime inspectedAt;

    private LocalDateTime createdAt;
}
