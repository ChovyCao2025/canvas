package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseFieldPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_field_policy")
public class CdpWarehouseFieldPolicyDO {

    /** CDP数仓字段策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓字段策略数据集业务键 */
    private String datasetKey;

    /** CDP数仓字段策略字段业务键 */
    private String fieldKey;

    /** CDP数仓字段策略物理名称 */
    private String physicalName;

    /** CDP数仓字段策略列名称 */
    private String columnName;

    /** CDP数仓字段策略值类型 */
    private String valueType;

    /** CDP数仓字段策略语义类型 */
    private String semanticType;

    /** CDP数仓字段策略PII级别 */
    private String piiLevel;

    /** CDP数仓字段策略访问策略 */
    private String accessPolicy;

    /** CDP数仓字段策略最小角色 */
    private String minRole;

    /** CDP数仓字段策略允许使用次数 */
    private String allowedUsages;

    /** CDP数仓字段策略脱敏策略 */
    private String maskStrategy;

    /** CDP数仓字段策略生命周期状态 */
    private String lifecycleStatus;

    /** CDP数仓字段策略负责人姓名 */
    private String ownerName;

    /** CDP数仓字段策略说明 */
    private String description;

    /** CDP数仓字段策略创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓字段策略最后更新时间 */
    private LocalDateTime updatedAt;
}
