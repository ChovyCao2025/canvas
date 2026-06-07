package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_provider_oauth_authorization_event")
public class MarketingMonitorProviderOAuthAuthorizationEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long authorizationId;

    private String authState;

    private String credentialKey;

    private String eventType;

    private String status;

    private String metadataJson;

    private String errorMessage;

    private String createdBy;

    private LocalDateTime createdAt;
}
