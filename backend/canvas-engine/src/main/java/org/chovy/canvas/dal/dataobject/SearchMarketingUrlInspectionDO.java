package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("search_marketing_url_inspection")
public class SearchMarketingUrlInspectionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private String provider;

    private String pageUrl;

    private String pageUrlHash;

    private LocalDate inspectionDate;

    private String indexedState;

    private String crawlState;

    private String canonicalUrl;

    private String sitemapState;

    private String mobileUsabilityState;

    private LocalDateTime lastCrawlAt;

    private String evidenceJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
