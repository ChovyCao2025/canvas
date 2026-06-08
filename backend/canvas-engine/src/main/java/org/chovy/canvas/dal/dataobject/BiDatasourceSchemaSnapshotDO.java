package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasourceSchemaSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_datasource_schema_snapshot")
public class BiDatasourceSchemaSnapshotDO {

    /** BI数据源结构快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的数据来源配置 ID */
    private Long dataSourceConfigId;

    /** BI数据源结构快照来源业务键 */
    private String sourceKey;

    /** BI数据源结构快照连接器类型 */
    private String connectorType;

    /** BI数据源结构快照结构定义 JSON */
    private String schemaJson;

    /** BI数据源结构快照同步状态 */
    private String syncStatus;

    /** BI数据源结构快照错误信息 */
    private String errorMessage;

    /** BI数据源结构快照表数量 */
    private Integer tableCount;

    /** BI数据源结构快照列数量 */
    private Integer columnCount;

    /** BI数据源结构快照同步人 */
    private String syncedBy;

    /** BI数据源结构快照同步时间 */
    private LocalDateTime syncedAt;

    /** BI数据源结构快照创建时间 */
    private LocalDateTime createdAt;
}
