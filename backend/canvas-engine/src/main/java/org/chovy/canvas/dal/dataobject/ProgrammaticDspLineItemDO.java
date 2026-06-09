package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProgrammaticDspLineItemDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("programmatic_dsp_line_item")
public class ProgrammaticDspLineItemDO {

    /** 程序化DSP行事项主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的席位 ID */
    private Long seatId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 程序化DSP行事项行事项业务键 */
    private String lineItemKey;

    /** 程序化DSP行事项行事项名称 */
    private String lineItemName;

    /** 程序化DSP行事项出价策略 */
    private String bidStrategy;

    /** 程序化DSP行事项最大出价CPM */
    private BigDecimal maxBidCpm;

    /** 程序化DSP行事项每日预算金额 */
    private BigDecimal dailyBudgetAmount;

    /** 程序化DSP行事项总计预算金额 */
    private BigDecimal totalBudgetAmount;

    /** 程序化DSP行事项节奏模式 */
    private String pacingMode;

    /** 程序化DSP行事项定向明细 JSON */
    private String targetingJson;

    /** 程序化DSP行事项频次上限 */
    private Integer frequencyCap;

    /** 程序化DSP行事项当前状态 */
    private String status;

    /** 程序化DSP行事项扩展元数据 JSON */
    private String metadataJson;

    /** 程序化DSP行事项创建人 */
    private String createdBy;

    /** 程序化DSP行事项创建时间 */
    private LocalDateTime createdAt;

    /** 程序化DSP行事项最后更新时间 */
    private LocalDateTime updatedAt;
}
