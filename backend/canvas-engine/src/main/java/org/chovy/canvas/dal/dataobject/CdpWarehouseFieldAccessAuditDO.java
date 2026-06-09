package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseFieldAccessAuditDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_field_access_audit")
public class CdpWarehouseFieldAccessAuditDO {

    /** CDP数仓字段访问审计主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓字段访问审计数据集业务键 */
    private String datasetKey;

    /** CDP数仓字段访问审计字段业务键 */
    private String fieldKey;

    /** 关联的操作人 ID */
    private String actorId;

    /** CDP数仓字段访问审计操作人角色 */
    private String actorRole;

    /** CDP数仓字段访问审计动作业务键 */
    private String actionKey;

    /** CDP数仓字段访问审计决策 */
    private String decision;

    /** CDP数仓字段访问审计原因说明 */
    private String reason;

    /** CDP数仓字段访问审计创建时间 */
    private LocalDateTime createdAt;
}
