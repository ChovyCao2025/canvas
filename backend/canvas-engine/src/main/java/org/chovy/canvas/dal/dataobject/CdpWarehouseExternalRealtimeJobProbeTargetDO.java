package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseExternalRealtimeJobProbeTargetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_external_realtime_job_probe_target")
public class CdpWarehouseExternalRealtimeJobProbeTargetDO {

    /** CDP数仓外部实时任务探测目标主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓外部实时任务探测目标管道业务键 */
    private String pipelineKey;

    /** CDP数仓外部实时任务探测目标任务业务键 */
    private String jobKey;

    /** CDP数仓外部实时任务探测目标引擎类型 */
    private String engineType;

    /** CDP数仓外部实时任务探测目标端点URL */
    private String endpointUrl;

    /** CDP数仓外部实时任务探测目标认证引用 */
    private String authRef;

    /** 关联的外部任务 ID */
    private String externalJobId;

    /** CDP数仓外部实时任务探测目标连接器名称 */
    private String connectorName;

    /** CDP数仓外部实时任务探测目标部署引用 */
    private String deploymentRef;

    /** CDP数仓外部实时任务探测目标是否启用 */
    private Integer enabled;

    /** CDP数仓外部实时任务探测目标负责人姓名 */
    private String ownerName;

    /** CDP数仓外部实时任务探测目标最大陈旧度秒数 */
    private Integer maxStalenessSeconds;

    /** CDP数仓外部实时任务探测目标配置 JSON */
    private String configJson;

    /** CDP数仓外部实时任务探测目标最近探测时间 */
    private LocalDateTime lastProbedAt;

    /** CDP数仓外部实时任务探测目标最近探测状态 */
    private String lastProbeStatus;

    /** CDP数仓外部实时任务探测目标最近探测消息 */
    private String lastProbeMessage;

    /** CDP数仓外部实时任务探测目标创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓外部实时任务探测目标最后更新时间 */
    private LocalDateTime updatedAt;
}
