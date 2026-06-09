package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SearchMarketingKeywordDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_keyword")
public class SearchMarketingKeywordDO {

    /** 搜索营销关键词主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销关键词触达渠道 */
    private String channel;

    /** 搜索营销关键词关键词文本 */
    private String keywordText;

    /** 搜索营销关键词关键词业务键 */
    private String keywordKey;

    /** 搜索营销关键词匹配类型 */
    private String matchType;

    /** 搜索营销关键词落地页页面URL */
    private String landingPageUrl;

    /** 搜索营销关键词落地页页面URL哈希 */
    private String landingPageUrlHash;

    /** 搜索营销关键词搜索意图 */
    private String searchIntent;

    /** 搜索营销关键词标签明细 JSON */
    private String labelsJson;

    /** 搜索营销关键词当前状态 */
    private String status;

    /** 搜索营销关键词扩展元数据 JSON */
    private String metadataJson;

    /** 搜索营销关键词创建人 */
    private String createdBy;

    /** 搜索营销关键词创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销关键词最后更新时间 */
    private LocalDateTime updatedAt;
}
