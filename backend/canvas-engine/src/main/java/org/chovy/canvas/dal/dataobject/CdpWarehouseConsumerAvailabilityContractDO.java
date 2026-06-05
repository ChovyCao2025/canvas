package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_consumer_availability_contract")
public class CdpWarehouseConsumerAvailabilityContractDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String contractKey;

    private String consumerType;

    private String consumerRef;

    private String datasetKey;

    private String metricKey;

    private String requiredMode;

    private String requiredAssetsJson;

    private String gatePolicy;

    private Integer warnToleranceMinutes;

    private String status;

    private String ownerName;

    private String description;

    private LocalDateTime lastEvaluatedAt;

    private String lastStatus;

    private String lastMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
