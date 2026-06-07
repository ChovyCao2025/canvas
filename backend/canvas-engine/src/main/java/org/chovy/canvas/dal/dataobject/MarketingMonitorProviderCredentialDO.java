package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_provider_credential")
public class MarketingMonitorProviderCredentialDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String credentialKey;

    private String providerType;

    private String authType;

    private String displayName;

    private String status;

    private String tokenType;

    private String scopesJson;

    private String accessTokenPrefix;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String accessTokenHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String accessTokenCiphertext;

    private String refreshTokenPrefix;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String refreshTokenHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String refreshTokenCiphertext;

    private String apiKeyPrefix;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKeyHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKeyCiphertext;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientIdCiphertext;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientSecretCiphertext;

    private String refreshEndpoint;

    private String revokeEndpoint;

    private LocalDateTime expiresAt;

    private LocalDateTime refreshTokenExpiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime lastRefreshedAt;

    private Integer refreshAttemptCount;

    private String lastRefreshStatus;

    private String lastRefreshError;

    private String lastRevokeStatus;

    private String lastRevokeError;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
