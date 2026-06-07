package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_sentiment_analysis")
public class MarketingSentimentAnalysisDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long itemId;

    private String sentimentLabel;

    private BigDecimal sentimentScore;

    private BigDecimal confidence;

    private String modelKey;

    private String modelVersion;

    private String keywordHitsJson;

    private LocalDateTime createdAt;
}
