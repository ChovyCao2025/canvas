package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_integration_contract")
public class MarketingIntegrationContractDO {

    /** 营销集成契约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销集成契约契约业务键 */
    private String contractKey;

    /** 营销集成契约展示名称 */
    private String displayName;

    /** 营销集成契约服务商族 */
    private String providerFamily;

    /** 营销集成契约来源能力业务键 */
    private String sourceCapabilityKey;

    /** 营销集成契约目标能力业务键 */
    private String targetCapabilityKey;

    /** 营销集成契约资产业务键 */
    private String assetKey;

    /** 营销集成契约指标方向 */
    private String direction;

    /** 营销集成契约环境 */
    private String environment;

    /** 营销集成契约认证模式 */
    private String authMode;

    /** 营销集成契约凭据依赖 */
    private String credentialDependency;

    /** 营销集成契约API根地址 */
    private String apiRoot;

    /** 营销集成契约负责团队 */
    private String ownerTeam;

    /** 营销集成契约当前状态 */
    private String status;

    /** 营销集成契约SLA层级 */
    private String slaTier;

    /** 营销集成契约超时时间毫秒 */
    private Integer timeoutMs;

    /** 营销集成契约重试策略明细 JSON */
    private String retryPolicyJson;

    /** 营销集成契约结构契约明细 JSON */
    private String schemaContractJson;

    /** 营销集成契约扩展元数据 JSON */
    private String metadataJson;

    /** 营销集成契约创建人 */
    private String createdBy;

    /** 营销集成契约最后更新人 */
    private String updatedBy;

    /** 营销集成契约创建时间 */
    private LocalDateTime createdAt;

    /** 营销集成契约最后更新时间 */
    private LocalDateTime updatedAt;
}
