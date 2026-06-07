package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("creator_profile")
public class CreatorProfileDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String handle;

    private String handleKey;

    private String displayName;

    private String creatorTier;

    private String primaryChannel;

    private Long followerCount;

    private BigDecimal avgEngagementRate;

    private String tagsJson;

    private String status;

    private String riskStatus;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
