package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SearchMarketingImpactWindowDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_impact_window")
public class SearchMarketingImpactWindowDO {

    /** 搜索营销影响窗口主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的机会 ID */
    private Long opportunityId;

    /** 关联的变更 ID */
    private Long mutationId;

    /** 搜索营销影响窗口来源 ID */
    private Long sourceId;

    /** 关联的关键词 ID */
    private Long keywordId;

    /** 搜索营销影响窗口页面URL哈希 */
    private String pageUrlHash;

    /** 搜索营销影响窗口基线开始日期 */
    private LocalDate baselineStartDate;

    /** 搜索营销影响窗口基线结束日期 */
    private LocalDate baselineEndDate;

    /** 搜索营销影响窗口投放开始日期 */
    private LocalDate postStartDate;

    /** 搜索营销影响窗口投放结束日期 */
    private LocalDate postEndDate;

    /** 搜索营销影响窗口当前状态 */
    private String status;

    /** 搜索营销影响窗口决策 */
    private String decision;

    /** 搜索营销影响窗口置信度 */
    private BigDecimal confidence;

    /** 搜索营销影响窗口指标增量明细 JSON */
    private String metricDeltaJson;

    /** 搜索营销影响窗口证据明细 JSON */
    private String evidenceJson;

    /** 搜索营销影响窗口截止时间 */
    private LocalDateTime dueAt;

    /** 搜索营销影响窗口评估时间 */
    private LocalDateTime evaluatedAt;

    /** 搜索营销影响窗口创建人 */
    private String createdBy;

    /** 搜索营销影响窗口创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销影响窗口最后更新时间 */
    private LocalDateTime updatedAt;
}
