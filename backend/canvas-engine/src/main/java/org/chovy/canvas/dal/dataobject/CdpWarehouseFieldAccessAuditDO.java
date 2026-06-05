package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_field_access_audit")
public class CdpWarehouseFieldAccessAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String datasetKey;

    private String fieldKey;

    private String actorId;

    private String actorRole;

    private String actionKey;

    private String decision;

    private String reason;

    private LocalDateTime createdAt;
}
