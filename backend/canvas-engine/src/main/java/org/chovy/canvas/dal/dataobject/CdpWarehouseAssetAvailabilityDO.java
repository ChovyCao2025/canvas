package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseAssetAvailabilityDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_asset_availability")
public class CdpWarehouseAssetAvailabilityDO {

    /** CDP数仓资产可用性主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓资产可用性资产类型 */
    private String assetType;

    /** CDP数仓资产可用性资产业务键 */
    private String assetKey;

    /** CDP数仓资产可用性可用性模式 */
    private String availabilityMode;

    /** CDP数仓资产可用性窗口开始时间 */
    private LocalDateTime windowStart;

    /** CDP数仓资产可用性窗口结束时间 */
    private LocalDateTime windowEnd;

    /** CDP数仓资产可用性可用截止 */
    private LocalDateTime availableUntil;

    /** CDP数仓资产可用性当前状态 */
    private String status;

    /** CDP数仓资产可用性证据来源 */
    private String evidenceSource;

    /** CDP数仓资产可用性证据引用 */
    private String evidenceRef;

    /** CDP数仓资产可用性原因说明 */
    private String reason;

    /** CDP数仓资产可用性观测时间 */
    private LocalDateTime observedAt;

    /** CDP数仓资产可用性创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓资产可用性最后更新时间 */
    private LocalDateTime updatedAt;
}
