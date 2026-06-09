package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SearchMarketingUrlInspectionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_url_inspection")
public class SearchMarketingUrlInspectionDO {

    /** 搜索营销URL巡检主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销URL巡检来源 ID */
    private Long sourceId;

    /** 搜索营销URL巡检服务商 */
    private String provider;

    /** 搜索营销URL巡检页面URL */
    private String pageUrl;

    /** 搜索营销URL巡检页面URL哈希 */
    private String pageUrlHash;

    /** 搜索营销URL巡检巡检日期 */
    private LocalDate inspectionDate;

    /** 搜索营销URL巡检已索引状态 */
    private String indexedState;

    /** 搜索营销URL巡检抓取状态 */
    private String crawlState;

    /** 搜索营销URL巡检规范URL */
    private String canonicalUrl;

    /** 搜索营销URL巡检站点地图状态 */
    private String sitemapState;

    /** 搜索营销URL巡检移动端可用性状态 */
    private String mobileUsabilityState;

    /** 搜索营销URL巡检最近抓取时间 */
    private LocalDateTime lastCrawlAt;

    /** 搜索营销URL巡检证据明细 JSON */
    private String evidenceJson;

    /** 搜索营销URL巡检创建人 */
    private String createdBy;

    /** 搜索营销URL巡检创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销URL巡检最后更新时间 */
    private LocalDateTime updatedAt;
}
