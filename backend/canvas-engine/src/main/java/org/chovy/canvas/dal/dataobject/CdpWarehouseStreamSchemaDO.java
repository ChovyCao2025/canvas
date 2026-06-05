package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_stream_schema")
public class CdpWarehouseStreamSchemaDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String pipelineKey;

    private String schemaRole;

    private String schemaVersion;

    private String schemaHash;

    private String schemaJson;

    private String compatibilityPolicy;

    private String compatibilityStatus;

    private String compatibilityReason;

    private Integer active;

    private String registeredBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
