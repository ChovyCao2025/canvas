package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_line_item")
public class ProgrammaticDspLineItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long seatId;

    private Long campaignId;

    private String lineItemKey;

    private String lineItemName;

    private String bidStrategy;

    private BigDecimal maxBidCpm;

    private BigDecimal dailyBudgetAmount;

    private BigDecimal totalBudgetAmount;

    private String pacingMode;

    private String targetingJson;

    private Integer frequencyCap;

    private String status;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
