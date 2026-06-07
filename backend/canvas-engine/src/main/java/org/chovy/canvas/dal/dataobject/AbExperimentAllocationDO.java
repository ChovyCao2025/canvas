package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_allocation")
public class AbExperimentAllocationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long experimentId;

    private Long layerId;

    private String variantKey;

    private BigDecimal allocationPct;

    private Integer bucketStart;

    private Integer bucketEnd;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
