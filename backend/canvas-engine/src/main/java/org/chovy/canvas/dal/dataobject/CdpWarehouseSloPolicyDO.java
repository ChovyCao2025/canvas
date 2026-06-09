package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseSloPolicyDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_slo_policy")
public class CdpWarehouseSloPolicyDO {

    /** CDP数仓SLO策略主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓SLO策略策略业务键 */
    private String policyKey;

    /** CDP数仓SLO策略展示名称 */
    private String displayName;

    /** CDP数仓SLO策略离线预警运行差距分钟 */
    private Integer offlineWarnRunGapMinutes;

    /** CDP数仓SLO策略离线失败运行差距分钟 */
    private Integer offlineFailRunGapMinutes;

    /** CDP数仓SLO策略离线预警水位延迟分钟 */
    private Integer offlineWarnWatermarkLagMinutes;

    /** CDP数仓SLO策略离线失败水位延迟分钟 */
    private Integer offlineFailWatermarkLagMinutes;

    /** CDP数仓SLO策略人群预警运行差距分钟 */
    private Integer audienceWarnRunGapMinutes;

    /** CDP数仓SLO策略人群失败运行差距分钟 */
    private Integer audienceFailRunGapMinutes;

    /** CDP数仓SLO策略当前状态 */
    private String status;

    /** CDP数仓SLO策略负责人姓名 */
    private String ownerName;

    /** CDP数仓SLO策略说明 */
    private String description;

    /** CDP数仓SLO策略创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓SLO策略最后更新时间 */
    private LocalDateTime updatedAt;
}
