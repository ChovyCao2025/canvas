package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SearchMarketingSourceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_source")
public class SearchMarketingSourceDO {

    /** 搜索营销来源主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销来源服务商 */
    private String provider;

    /** 搜索营销来源来源业务键 */
    private String sourceKey;

    /** 搜索营销来源展示名称 */
    private String displayName;

    /** 搜索营销来源触达渠道 */
    private String channel;

    /** 关联的外部账户 ID */
    private String externalAccountId;

    /** 搜索营销来源站点URL */
    private String siteUrl;

    /** 搜索营销来源币种 */
    private String currency;

    /** 搜索营销来源时区 */
    private String timezone;

    /** 搜索营销来源是否启用 */
    private Integer enabled;

    /** 搜索营销来源扩展元数据 JSON */
    private String metadataJson;

    /** 搜索营销来源创建人 */
    private String createdBy;

    /** 搜索营销来源创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销来源最后更新时间 */
    private LocalDateTime updatedAt;
}
