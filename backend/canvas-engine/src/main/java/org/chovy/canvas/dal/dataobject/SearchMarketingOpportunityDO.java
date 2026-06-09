package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SearchMarketingOpportunityDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_opportunity")
public class SearchMarketingOpportunityDO {

    /** 搜索营销机会主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销机会来源 ID */
    private Long sourceId;

    /** 关联的关键词 ID */
    private Long keywordId;

    /** 搜索营销机会触达渠道 */
    private String channel;

    /** 搜索营销机会机会类型 */
    private String opportunityType;

    /** 搜索营销机会快照日期 */
    private LocalDate snapshotDate;

    /** 搜索营销机会严重级别 */
    private String severity;

    /** 搜索营销机会当前状态 */
    private String status;

    /** 搜索营销机会推荐 */
    private String recommendation;

    /** 搜索营销机会影响评分 */
    private BigDecimal impactScore;

    /** 搜索营销机会证据明细 JSON */
    private String evidenceJson;

    /** 搜索营销机会创建人 */
    private String createdBy;

    /** 搜索营销机会创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销机会最后更新时间 */
    private LocalDateTime updatedAt;
}
