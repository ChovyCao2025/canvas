package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_marketing_impact_window")
public class SearchMarketingImpactWindowDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long opportunityId;

    private Long mutationId;

    private Long sourceId;

    private Long keywordId;

    private String pageUrlHash;

    private LocalDate baselineStartDate;

    private LocalDate baselineEndDate;

    private LocalDate postStartDate;

    private LocalDate postEndDate;

    private String status;

    private String decision;

    private BigDecimal confidence;

    private String metricDeltaJson;

    private String evidenceJson;

    private LocalDateTime dueAt;

    private LocalDateTime evaluatedAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
