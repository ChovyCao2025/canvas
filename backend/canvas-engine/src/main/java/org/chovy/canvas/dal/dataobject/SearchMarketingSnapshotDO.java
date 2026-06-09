package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SearchMarketingSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("search_marketing_snapshot")
public class SearchMarketingSnapshotDO {

    /** 搜索营销快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 搜索营销快照来源 ID */
    private Long sourceId;

    /** 关联的关键词 ID */
    private Long keywordId;

    /** 搜索营销快照触达渠道 */
    private String channel;

    /** 搜索营销快照快照日期 */
    private LocalDate snapshotDate;

    /** 搜索营销快照设备 */
    private String device;

    /** 搜索营销快照国家 */
    private String country;

    /** 搜索营销快照查询分组业务键 */
    private String queryGroupKey;

    /** 搜索营销快照曝光数量 */
    private Long impressionCount;

    /** 搜索营销快照点击数量 */
    private Long clickCount;

    /** 搜索营销快照成本金额 */
    private BigDecimal costAmount;

    /** 搜索营销快照转化数量 */
    private Long conversionCount;

    /** 搜索营销快照收入金额 */
    private BigDecimal revenueAmount;

    /** 搜索营销快照平均排名 */
    private BigDecimal averagePosition;

    /** 搜索营销快照扩展元数据 JSON */
    private String metadataJson;

    /** 搜索营销快照创建人 */
    private String createdBy;

    /** 搜索营销快照创建时间 */
    private LocalDateTime createdAt;

    /** 搜索营销快照最后更新时间 */
    private LocalDateTime updatedAt;
}
