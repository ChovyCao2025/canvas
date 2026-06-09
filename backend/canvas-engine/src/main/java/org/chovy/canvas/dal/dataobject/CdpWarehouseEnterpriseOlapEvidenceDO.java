package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseEnterpriseOlapEvidenceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_enterprise_olap_evidence")
public class CdpWarehouseEnterpriseOlapEvidenceDO {

    /** CDP数仓企业OLAP证据主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP数仓企业OLAP证据证据业务键 */
    private String evidenceKey;
    /** CDP数仓企业OLAP证据来源 */
    private String source;
    /** CDP数仓企业OLAP证据当前状态 */
    private String status;
    /** CDP数仓企业OLAP证据原因说明 */
    private String reason;
    /** CDP数仓企业OLAP证据测量时间 */
    private LocalDateTime measuredAt;
    /** CDP数仓企业OLAP证据过期时间 */
    private LocalDateTime expiresAt;
    /** CDP数仓企业OLAP证据证据明细 JSON */
    private String evidenceJson;
    /** CDP数仓企业OLAP证据创建人 */
    private String createdBy;

    /** CDP数仓企业OLAP证据创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
