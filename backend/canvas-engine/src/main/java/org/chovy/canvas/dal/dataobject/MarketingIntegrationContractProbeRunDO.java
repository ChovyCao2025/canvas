package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_integration_contract_probe_run")
public class MarketingIntegrationContractProbeRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long contractId;

    private String contractKey;

    private String providerFamily;

    private String probeKey;

    private String environment;

    private String status;

    private Integer httpStatusCode;

    private Long latencyMs;

    private String errorType;

    private String problemTypeUri;

    private String problemTitle;

    private String problemDetail;

    private String errorMessage;

    private String summary;

    private LocalDateTime observedAt;

    private String evidenceJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
