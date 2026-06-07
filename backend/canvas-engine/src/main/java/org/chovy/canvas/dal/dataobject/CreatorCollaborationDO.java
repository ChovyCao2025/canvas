package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("creator_collaboration")
public class CreatorCollaborationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long campaignId;

    private Long creatorId;

    private String offerType;

    private BigDecimal fixedFeeAmount;

    private BigDecimal commissionRate;

    private String trackingLink;

    private String discountCode;

    private String status;

    private String permissionsMetadataJson;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
