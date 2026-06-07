package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_marketing_opportunity")
public class SearchMarketingOpportunityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private Long keywordId;

    private String channel;

    private String opportunityType;

    private LocalDate snapshotDate;

    private String severity;

    private String status;

    private String recommendation;

    private BigDecimal impactScore;

    private String evidenceJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
