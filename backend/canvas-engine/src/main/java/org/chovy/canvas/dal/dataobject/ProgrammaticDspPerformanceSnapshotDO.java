package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_performance_snapshot")
public class ProgrammaticDspPerformanceSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long seatId;

    private Long campaignId;

    private Long lineItemId;

    private LocalDate snapshotDate;

    private Long bidCount;

    private Long winCount;

    private Long impressionCount;

    private Long clickCount;

    private Long conversionCount;

    private Long viewableImpressionCount;

    private BigDecimal spendAmount;

    private BigDecimal revenueAmount;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
