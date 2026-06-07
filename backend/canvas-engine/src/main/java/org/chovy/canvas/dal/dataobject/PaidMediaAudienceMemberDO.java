package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paid_media_audience_member")
public class PaidMediaAudienceMemberDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long runId;

    private Long destinationId;

    private Long audienceId;

    private String provider;

    private String userId;

    private String identifierType;

    private String identifierHash;

    private String eligibilityStatus;

    private String reason;

    private LocalDateTime syncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
