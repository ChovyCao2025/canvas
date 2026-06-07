package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_provider_oauth_authorization")
public class MarketingMonitorProviderOAuthAuthorizationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String authState;

    private String credentialKey;

    private String providerType;

    private String authType;

    private String displayName;

    private String status;

    private String authorizeEndpoint;

    private String tokenEndpoint;

    private String revokeEndpoint;

    private String redirectUri;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientIdCiphertext;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientSecretCiphertext;

    private String scopesJson;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String codeVerifierCiphertext;

    private String codeChallenge;

    private String codeChallengeMethod;

    private String authorizeParamsJson;

    private String tokenType;

    private Long credentialId;

    private String providerError;

    private String providerErrorDescription;

    private Integer lastHttpStatus;

    private String lastErrorMessage;

    private LocalDateTime expiresAt;

    private LocalDateTime completedAt;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
