package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseQualityCheckDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_quality_check")
public class CdpWarehouseQualityCheckDO {

    /** CDP数仓质量检查主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓质量检查检查类型 */
    private String checkType;

    /** CDP数仓质量检查当前状态 */
    private String status;

    /** CDP数仓质量检查来源数量 */
    private Long sourceCount;

    /** CDP数仓质量检查数仓数量 */
    private Long warehouseCount;

    /** CDP数仓质量检查差异数量 */
    private Long diffCount;

    /** CDP数仓质量检查窗口开始时间 */
    private LocalDateTime windowStart;

    /** CDP数仓质量检查窗口结束时间 */
    private LocalDateTime windowEnd;

    /** CDP数仓质量检查阈值值 */
    private Long thresholdValue;

    /** CDP数仓质量检查明细明细 JSON */
    private String detailsJson;

    /** CDP数仓质量检查检查时间 */
    private LocalDateTime checkedAt;

    /** CDP数仓质量检查创建人 */
    private String createdBy;
}
