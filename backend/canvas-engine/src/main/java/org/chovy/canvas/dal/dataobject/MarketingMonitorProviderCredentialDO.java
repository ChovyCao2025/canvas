package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * MarketingMonitorProviderCredentialDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_provider_credential")
public class MarketingMonitorProviderCredentialDO {

    /** 营销监控服务商凭据主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控服务商凭据凭据业务键 */
    private String credentialKey;

    /** 营销监控服务商凭据服务商类型 */
    private String providerType;

    /** 营销监控服务商凭据认证类型 */
    private String authType;

    /** 营销监控服务商凭据展示名称 */
    private String displayName;

    /** 营销监控服务商凭据当前状态 */
    private String status;

    /** 营销监控服务商凭据令牌类型 */
    private String tokenType;

    /** 营销监控服务商凭据范围明细 JSON */
    private String scopesJson;

    /** 营销监控服务商凭据访问令牌前缀 */
    private String accessTokenPrefix;

    /** 营销监控服务商凭据访问令牌哈希 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String accessTokenHash;

    /** 营销监控服务商凭据访问令牌密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String accessTokenCiphertext;

    /** 营销监控服务商凭据刷新令牌前缀 */
    private String refreshTokenPrefix;

    /** 营销监控服务商凭据刷新令牌哈希 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String refreshTokenHash;

    /** 营销监控服务商凭据刷新令牌密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String refreshTokenCiphertext;

    /** 营销监控服务商凭据API键前缀 */
    private String apiKeyPrefix;

    /** 营销监控服务商凭据API键哈希 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKeyHash;

    /** 营销监控服务商凭据API键密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKeyCiphertext;

    /** 营销监控服务商凭据客户端ID密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientIdCiphertext;

    /** 营销监控服务商凭据客户端密钥密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientSecretCiphertext;

    /** 营销监控服务商凭据刷新端点 */
    private String refreshEndpoint;

    /** 营销监控服务商凭据撤销端点 */
    private String revokeEndpoint;

    /** 营销监控服务商凭据过期时间 */
    private LocalDateTime expiresAt;

    /** 营销监控服务商凭据刷新令牌过期时间 */
    private LocalDateTime refreshTokenExpiresAt;

    /** 营销监控服务商凭据吊销时间 */
    private LocalDateTime revokedAt;

    /** 营销监控服务商凭据最近刷新时间 */
    private LocalDateTime lastRefreshedAt;

    /** 营销监控服务商凭据刷新尝试数量 */
    private Integer refreshAttemptCount;

    /** 营销监控服务商凭据最近刷新状态 */
    private String lastRefreshStatus;

    /** 营销监控服务商凭据最近刷新错误 */
    private String lastRefreshError;

    /** 营销监控服务商凭据最近撤销状态 */
    private String lastRevokeStatus;

    /** 营销监控服务商凭据最近撤销错误 */
    private String lastRevokeError;

    /** 营销监控服务商凭据扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控服务商凭据创建人 */
    private String createdBy;

    /** 营销监控服务商凭据最后更新人 */
    private String updatedBy;

    /** 营销监控服务商凭据创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控服务商凭据最后更新时间 */
    private LocalDateTime updatedAt;
}
