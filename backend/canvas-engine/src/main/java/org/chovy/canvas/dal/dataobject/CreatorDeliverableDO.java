package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CreatorDeliverableDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("creator_deliverable")
public class CreatorDeliverableDO {

    /** 达人交付物主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的协作 ID */
    private Long collaborationId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 关联的达人 ID */
    private Long creatorId;

    /** 达人交付物交付物业务键 */
    private String deliverableKey;

    /** 达人交付物内容类型 */
    private String contentType;

    /** 达人交付物平台 */
    private String platform;

    /** 达人交付物截止时间 */
    private LocalDateTime dueAt;

    /** CREATORDELIVERABLEPOSTEDAT时间 */
    private LocalDateTime postedAt;

    /** 达人交付物内容URL */
    private String contentUrl;

    /** 达人交付物当前状态 */
    private String status;

    /** 达人交付物曝光数量 */
    private Long impressionCount;

    /** 达人交付物点赞数量 */
    private Long likeCount;

    /** 达人交付物备注数量 */
    private Long commentCount;

    /** 达人交付物分享数量 */
    private Long shareCount;

    /** 达人交付物收藏数量 */
    private Long saveCount;

    /** 达人交付物点击数量 */
    private Long clickCount;

    /** 达人交付物转化数量 */
    private Long conversionCount;

    /** 达人交付物收入金额 */
    private BigDecimal revenueAmount;

    /** 达人交付物扩展元数据 JSON */
    private String metadataJson;

    /** 达人交付物创建人 */
    private String createdBy;

    /** 达人交付物创建时间 */
    private LocalDateTime createdAt;

    /** 达人交付物最后更新时间 */
    private LocalDateTime updatedAt;
}
