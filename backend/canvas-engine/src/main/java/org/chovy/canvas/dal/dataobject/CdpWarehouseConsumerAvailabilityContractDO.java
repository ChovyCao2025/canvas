package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseConsumerAvailabilityContractDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_consumer_availability_contract")
public class CdpWarehouseConsumerAvailabilityContractDO {

    /** CDP数仓消费方可用性契约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓消费方可用性契约契约业务键 */
    private String contractKey;

    /** CDP数仓消费方可用性契约消费方类型 */
    private String consumerType;

    /** CDP数仓消费方可用性契约消费方引用 */
    private String consumerRef;

    /** CDP数仓消费方可用性契约数据集业务键 */
    private String datasetKey;

    /** CDP数仓消费方可用性契约指标标识 */
    private String metricKey;

    /** CDP数仓消费方可用性契约要求模式 */
    private String requiredMode;

    /** CDP数仓消费方可用性契约要求资产明细 JSON */
    private String requiredAssetsJson;

    /** CDP数仓消费方可用性契约闸口策略 */
    private String gatePolicy;

    /** CDP数仓消费方可用性契约预警容忍分钟 */
    private Integer warnToleranceMinutes;

    /** CDP数仓消费方可用性契约当前状态 */
    private String status;

    /** CDP数仓消费方可用性契约负责人姓名 */
    private String ownerName;

    /** CDP数仓消费方可用性契约说明 */
    private String description;

    /** CDP数仓消费方可用性契约最近评估时间 */
    private LocalDateTime lastEvaluatedAt;

    /** CDP数仓消费方可用性契约最近状态 */
    private String lastStatus;

    /** CDP数仓消费方可用性契约最近消息 */
    private String lastMessage;

    /** CDP数仓消费方可用性契约创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓消费方可用性契约最后更新时间 */
    private LocalDateTime updatedAt;
}
