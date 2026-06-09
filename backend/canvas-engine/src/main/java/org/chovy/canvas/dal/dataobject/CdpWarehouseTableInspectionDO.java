package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseTableInspectionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_table_inspection")
public class CdpWarehouseTableInspectionDO {

    /** CDP数仓表巡检主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓表巡检表业务键 */
    private String tableKey;

    /** CDP数仓表巡检物理名称 */
    private String physicalName;

    /** CDP数仓表巡检当前状态 */
    private String status;

    /** CDP数仓表巡检检查项 */
    private Integer checkedItems;

    /** CDP数仓表巡检违规数量 */
    private Integer violationCount;

    /** CDP数仓表巡检消息 */
    private String message;

    /** CDP数仓表巡检违规项明细 JSON */
    private String violationsJson;

    /** CDP数仓表巡检DDL资产路径 */
    private String ddlAssetPath;

    /** CDP数仓表巡检人 */
    private String inspectedBy;

    /** CDP数仓表巡检时间 */
    private LocalDateTime inspectedAt;

    /** CDP数仓表巡检创建时间 */
    private LocalDateTime createdAt;
}
