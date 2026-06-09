package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingSentimentAnalysisDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_sentiment_analysis")
public class MarketingSentimentAnalysisDO {

    /** 营销情绪分析主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的事项 ID */
    private Long itemId;

    /** 营销情绪分析情绪标签 */
    private String sentimentLabel;

    /** 营销情绪分析情绪评分 */
    private BigDecimal sentimentScore;

    /** 营销情绪分析置信度 */
    private BigDecimal confidence;

    /** 营销情绪分析模型标识 */
    private String modelKey;

    /** 营销情绪分析MODELVERSION */
    private String modelVersion;

    /** 营销情绪分析关键词HITSJSON明细 JSON */
    private String keywordHitsJson;

    /** 营销情绪分析创建时间 */
    private LocalDateTime createdAt;
}
