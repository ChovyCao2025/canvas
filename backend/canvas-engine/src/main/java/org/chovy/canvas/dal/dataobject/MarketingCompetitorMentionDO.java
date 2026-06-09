package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingCompetitorMentionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_competitor_mention")
public class MarketingCompetitorMentionDO {

    /** 营销竞品提及主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的事项 ID */
    private Long itemId;

    /** 营销竞品提及竞品业务键 */
    private String competitorKey;

    /** 营销竞品提及竞品名称 */
    private String competitorName;

    /** 营销竞品提及匹配词条明细 JSON */
    private String matchedTermsJson;

    /** 营销竞品提及情绪标签 */
    private String sentimentLabel;

    /** 营销竞品提及情绪评分 */
    private BigDecimal sentimentScore;

    /** 营销竞品提及创建时间 */
    private LocalDateTime createdAt;
}
