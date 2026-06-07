package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_marketing_snapshot")
public class SearchMarketingSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private Long keywordId;

    private String channel;

    private LocalDate snapshotDate;

    private String device;

    private String country;

    private String queryGroupKey;

    private Long impressionCount;

    private Long clickCount;

    private BigDecimal costAmount;

    private Long conversionCount;

    private BigDecimal revenueAmount;

    private BigDecimal averagePosition;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
