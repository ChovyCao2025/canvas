package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paid_media_audience_destination")
public class PaidMediaAudienceDestinationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String provider;

    private String destinationKey;

    private String displayName;

    private String accountId;

    private String externalAudienceId;

    private String identifierTypesJson;

    private String consentChannel;

    private Integer enforceConsent;

    private Integer enabled;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
