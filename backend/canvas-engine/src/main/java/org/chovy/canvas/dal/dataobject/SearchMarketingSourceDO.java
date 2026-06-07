package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_marketing_source")
public class SearchMarketingSourceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String sourceKey;

    private String displayName;

    private String channel;

    private String externalAccountId;

    private String siteUrl;

    private String currency;

    private String timezone;

    private Integer enabled;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
