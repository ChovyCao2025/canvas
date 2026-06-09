package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * MarketingMonitorProviderOAuthAuthorizationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_provider_oauth_authorization")
public class MarketingMonitorProviderOAuthAuthorizationDO {

    /** 营销监控服务商OAuth授权主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控服务商OAuth授权认证状态 */
    private String authState;

    /** 营销监控服务商OAuth授权凭据业务键 */
    private String credentialKey;

    /** 营销监控服务商OAuth授权服务商类型 */
    private String providerType;

    /** 营销监控服务商OAuth授权认证类型 */
    private String authType;

    /** 营销监控服务商OAuth授权展示名称 */
    private String displayName;

    /** 营销监控服务商OAuth授权当前状态 */
    private String status;

    /** 营销监控服务商OAuth授权授权端点 */
    private String authorizeEndpoint;

    /** 营销监控服务商OAuth授权令牌端点 */
    private String tokenEndpoint;

    /** 营销监控服务商OAuth授权撤销端点 */
    private String revokeEndpoint;

    /** 营销监控服务商OAuth授权重定向URI */
    private String redirectUri;

    /** 营销监控服务商OAuth授权客户端ID密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientIdCiphertext;

    /** 营销监控服务商OAuth授权客户端密钥密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String clientSecretCiphertext;

    /** 营销监控服务商OAuth授权范围明细 JSON */
    private String scopesJson;

    /** 营销监控服务商OAuth授权编码校验器密文 */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String codeVerifierCiphertext;

    /** 营销监控服务商OAuth授权编码挑战 */
    private String codeChallenge;

    /** 营销监控服务商OAuth授权编码挑战方法 */
    private String codeChallengeMethod;

    /** 营销监控服务商OAuth授权授权参数明细 JSON */
    private String authorizeParamsJson;

    /** 营销监控服务商OAuth授权令牌类型 */
    private String tokenType;

    /** 关联的凭据 ID */
    private Long credentialId;

    /** 营销监控服务商OAuth授权服务商错误 */
    private String providerError;

    /** 营销监控服务商OAuth授权服务商错误说明 */
    private String providerErrorDescription;

    /** 营销监控服务商OAuth授权最近HTTP状态 */
    private Integer lastHttpStatus;

    /** 营销监控服务商OAuth授权最近错误消息 */
    private String lastErrorMessage;

    /** 营销监控服务商OAuth授权过期时间 */
    private LocalDateTime expiresAt;

    /** 营销监控服务商OAuth授权完成时间 */
    private LocalDateTime completedAt;

    /** 营销监控服务商OAuth授权扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控服务商OAuth授权创建人 */
    private String createdBy;

    /** 营销监控服务商OAuth授权最后更新人 */
    private String updatedBy;

    /** 营销监控服务商OAuth授权创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控服务商OAuth授权最后更新时间 */
    private LocalDateTime updatedAt;
}
