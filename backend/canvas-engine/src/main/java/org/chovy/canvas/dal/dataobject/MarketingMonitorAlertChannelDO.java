package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_alert_channel")
public class MarketingMonitorAlertChannelDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String channelKey;

    private String channelType;

    private String displayName;

    private String endpointUrl;

    private Integer enabled;

    private String minSeverity;

    private String alertTypesJson;

    private String signingMode;

    private String secretPrefix;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String secretHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String secretCiphertext;

    private String metadataJson;

    private Integer maxAttempts;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
