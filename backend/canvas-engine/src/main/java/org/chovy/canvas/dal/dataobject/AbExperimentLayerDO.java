package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_layer")
public class AbExperimentLayerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String layerKey;

    private String name;

    private String description;

    private BigDecimal trafficPct;

    private String salt;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
