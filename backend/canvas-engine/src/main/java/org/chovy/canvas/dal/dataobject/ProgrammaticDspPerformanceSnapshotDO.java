package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProgrammaticDspPerformanceSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("programmatic_dsp_performance_snapshot")
public class ProgrammaticDspPerformanceSnapshotDO {

    /** 程序化DSP效果SNAPSHOT主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的席位 ID */
    private Long seatId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 关联的行事项 ID */
    private Long lineItemId;

    /** 程序化DSP效果SNAPSHOTSNAPSHOTDATE */
    private LocalDate snapshotDate;

    /** 程序化DSP效果SNAPSHOTBIDCOUNT数量 */
    private Long bidCount;

    /** 程序化DSP效果SNAPSHOTWINCOUNT数量 */
    private Long winCount;

    /** 程序化DSP效果SNAPSHOTIMPRESSIONCOUNT数量 */
    private Long impressionCount;

    /** 程序化DSP效果SNAPSHOTCLICKCOUNT数量 */
    private Long clickCount;

    /** 程序化DSP效果SNAPSHOTCONVERSIONCOUNT数量 */
    private Long conversionCount;

    /** 程序化DSP效果SNAPSHOTVIEWABLEIMPRESSIONCOUNT数量 */
    private Long viewableImpressionCount;

    /** 程序化DSP效果SNAPSHOTSPENDAMOUNT */
    private BigDecimal spendAmount;

    /** 程序化DSP效果SNAPSHOTREVENUEAMOUNT */
    private BigDecimal revenueAmount;

    /** 程序化DSP效果SNAPSHOTMETADATAJSON明细 JSON */
    private String metadataJson;

    /** 程序化DSP效果SNAPSHOT创建人 */
    private String createdBy;

    /** 程序化DSP效果SNAPSHOT创建时间 */
    private LocalDateTime createdAt;

    /** 程序化DSP效果SNAPSHOT最后更新时间 */
    private LocalDateTime updatedAt;
}
