package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractProbeRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_integration_contract_probe_run")
public class MarketingIntegrationContractProbeRunDO {

    /** 营销集成契约探测运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的契约 ID */
    private Long contractId;

    /** 营销集成契约探测运行契约业务键 */
    private String contractKey;

    /** 营销集成契约探测运行服务商族 */
    private String providerFamily;

    /** 营销集成契约探测运行探测业务键 */
    private String probeKey;

    /** 营销集成契约探测运行环境 */
    private String environment;

    /** 营销集成契约探测运行当前状态 */
    private String status;

    /** 营销集成契约探测运行HTTP状态编码 */
    private Integer httpStatusCode;

    /** 营销集成契约探测运行延迟毫秒数 */
    private Long latencyMs;

    /** 营销集成契约探测运行错误类型 */
    private String errorType;

    /** 营销集成契约探测运行问题类型URI */
    private String problemTypeUri;

    /** 营销集成契约探测运行问题标题 */
    private String problemTitle;

    /** 营销集成契约探测运行问题明细 */
    private String problemDetail;

    /** 营销集成契约探测运行错误信息 */
    private String errorMessage;

    /** 营销集成契约探测运行摘要 */
    private String summary;

    /** 营销集成契约探测运行观测时间 */
    private LocalDateTime observedAt;

    /** 营销集成契约探测运行证据明细 JSON */
    private String evidenceJson;

    /** 营销集成契约探测运行创建人 */
    private String createdBy;

    /** 营销集成契约探测运行最后更新人 */
    private String updatedBy;

    /** 营销集成契约探测运行创建时间 */
    private LocalDateTime createdAt;

    /** 营销集成契约探测运行最后更新时间 */
    private LocalDateTime updatedAt;
}
