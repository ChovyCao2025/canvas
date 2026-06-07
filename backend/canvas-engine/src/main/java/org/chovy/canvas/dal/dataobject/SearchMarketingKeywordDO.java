package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_marketing_keyword")
public class SearchMarketingKeywordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String channel;

    private String keywordText;

    private String keywordKey;

    private String matchType;

    private String landingPageUrl;

    private String landingPageUrlHash;

    private String searchIntent;

    private String labelsJson;

    private String status;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
