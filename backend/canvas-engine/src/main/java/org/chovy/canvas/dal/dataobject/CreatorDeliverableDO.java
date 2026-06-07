package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("creator_deliverable")
public class CreatorDeliverableDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long collaborationId;

    private Long campaignId;

    private Long creatorId;

    private String deliverableKey;

    private String contentType;

    private String platform;

    private LocalDateTime dueAt;

    private LocalDateTime postedAt;

    private String contentUrl;

    private String status;

    private Long impressionCount;

    private Long likeCount;

    private Long commentCount;

    private Long shareCount;

    private Long saveCount;

    private Long clickCount;

    private Long conversionCount;

    private BigDecimal revenueAmount;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
