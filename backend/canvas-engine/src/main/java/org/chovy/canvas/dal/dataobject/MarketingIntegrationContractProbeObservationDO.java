package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractProbeObservationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_integration_contract_probe_observation")
public class MarketingIntegrationContractProbeObservationDO {

    /** 营销集成契约探测观测主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的契约 ID */
    private Long contractId;

    /** 关联的探测运行 ID */
    private Long probeRunId;

    /** 营销集成契约探测观测契约业务键 */
    private String contractKey;

    /** 营销集成契约探测观测服务商族 */
    private String providerFamily;

    /** 营销集成契约探测观测探测业务键 */
    private String probeKey;

    /** 营销集成契约探测观测环境 */
    private String environment;

    /** 营销集成契约探测观测当前状态 */
    private String status;

    /** 营销集成契约探测观测HTTP状态编码 */
    private Integer httpStatusCode;

    /** 营销集成契约探测观测延迟毫秒数 */
    private Long latencyMs;

    /** 营销集成契约探测观测错误类型 */
    private String errorType;

    /** 营销集成契约探测观测问题类型URI */
    private String problemTypeUri;

    /** 营销集成契约探测观测问题标题 */
    private String problemTitle;

    /** 营销集成契约探测观测问题明细 */
    private String problemDetail;

    /** 营销集成契约探测观测错误信息 */
    private String errorMessage;

    /** 营销集成契约探测观测摘要 */
    private String summary;

    /** 营销集成契约探测观测观测时间 */
    private LocalDateTime observedAt;

    /** 营销集成契约探测观测证据明细 JSON */
    private String evidenceJson;

    /** 营销集成契约探测观测创建人 */
    private String createdBy;

    /** 营销集成契约探测观测创建时间 */
    private LocalDateTime createdAt;
}
