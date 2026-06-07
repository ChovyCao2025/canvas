package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_source")
public class MarketingMonitorSourceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String sourceKey;

    private String sourceType;

    private String displayName;

    private Integer enabled;

    private String metadataJson;

    private Integer webhookEnabled;

    private String webhookSecretPrefix;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String webhookSecretHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String webhookSecretCiphertext;

    private Integer webhookSignatureToleranceSeconds;

    private Integer pollEnabled;

    private Integer pollIntervalMinutes;

    private String pollCursor;

    private LocalDateTime lastPolledAt;

    private LocalDateTime nextPollAt;

    private String lastPollStatus;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
