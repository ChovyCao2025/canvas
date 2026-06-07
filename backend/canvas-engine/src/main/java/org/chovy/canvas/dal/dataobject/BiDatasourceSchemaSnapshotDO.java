package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bi_datasource_schema_snapshot")
public class BiDatasourceSchemaSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long dataSourceConfigId;

    private String sourceKey;

    private String connectorType;

    private String schemaJson;

    private String syncStatus;

    private String errorMessage;

    private Integer tableCount;

    private Integer columnCount;

    private String syncedBy;

    private LocalDateTime syncedAt;

    private LocalDateTime createdAt;
}
