package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseStreamSchemaDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_stream_schema")
public class CdpWarehouseStreamSchemaDO {

    /** CDP数仓流结构主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓流结构管道业务键 */
    private String pipelineKey;

    /** CDP数仓流结构结构角色 */
    private String schemaRole;

    /** CDP数仓流结构结构版本 */
    private String schemaVersion;

    /** CDP数仓流结构结构哈希 */
    private String schemaHash;

    /** CDP数仓流结构结构定义 JSON */
    private String schemaJson;

    /** CDP数仓流结构兼容策略 */
    private String compatibilityPolicy;

    /** CDP数仓流结构兼容状态 */
    private String compatibilityStatus;

    /** CDP数仓流结构兼容原因 */
    private String compatibilityReason;

    /** CDP数仓流结构生效 */
    private Integer active;

    /** CDP数仓流结构注册人 */
    private String registeredBy;

    /** CDP数仓流结构创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓流结构最后更新时间 */
    private LocalDateTime updatedAt;
}
