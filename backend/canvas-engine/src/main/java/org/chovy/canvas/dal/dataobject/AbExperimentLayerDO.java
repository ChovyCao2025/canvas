package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AbExperimentLayerDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ab_experiment_layer")
public class AbExperimentLayerDO {

    /** A/B实验流量层主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** A/B实验流量层流量层业务键 */
    private String layerKey;

    /** A/B实验流量层名称 */
    private String name;

    /** A/B实验流量层说明 */
    private String description;

    /** A/B实验流量层流量占比 */
    private BigDecimal trafficPct;

    /** A/B实验流量层分流盐值 */
    private String salt;

    /** A/B实验流量层当前状态 */
    private String status;

    /** A/B实验流量层创建时间 */
    private LocalDateTime createdAt;

    /** A/B实验流量层最后更新时间 */
    private LocalDateTime updatedAt;
}
