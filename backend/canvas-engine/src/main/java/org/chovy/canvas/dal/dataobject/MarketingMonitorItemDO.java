package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingMonitorItemDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_item")
public class MarketingMonitorItemDO {

    /** 营销监控事项主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控事项来源 ID */
    private Long sourceId;

    /** 关联的外部事项 ID */
    private String externalItemId;

    /** 营销监控事项来源类型 */
    private String sourceType;

    /** 营销监控事项来源URL */
    private String sourceUrl;

    /** 营销监控事项作者业务键 */
    private String authorKey;

    /** 营销监控事项品牌业务键 */
    private String brandKey;

    /** 营销监控事项文本内容 */
    private String textContent;

    /** 营销监控事项语言 */
    private String language;

    /** 营销监控事项发布时间 */
    private LocalDateTime publishedAt;

    /** 营销监控事项入库时间 */
    private LocalDateTime ingestedAt;

    /** 营销监控事项原始载荷 JSON */
    private String rawPayloadJson;

    /** 营销监控事项创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控事项最后更新时间 */
    private LocalDateTime updatedAt;
}
