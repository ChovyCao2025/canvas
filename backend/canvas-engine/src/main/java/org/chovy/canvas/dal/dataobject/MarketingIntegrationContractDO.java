package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_integration_contract")
public class MarketingIntegrationContractDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String contractKey;

    private String displayName;

    private String providerFamily;

    private String sourceCapabilityKey;

    private String targetCapabilityKey;

    private String assetKey;

    private String direction;

    private String environment;

    private String authMode;

    private String credentialDependency;

    private String apiRoot;

    private String ownerTeam;

    private String status;

    private String slaTier;

    private Integer timeoutMs;

    private String retryPolicyJson;

    private String schemaContractJson;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
