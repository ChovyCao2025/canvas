package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseLineageEdgeDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_lineage_edge")
public class CdpWarehouseLineageEdgeDO {

    /** CDP数仓血缘边主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓血缘边上游数据集业务键 */
    private String upstreamDatasetKey;

    /** CDP数仓血缘边下游数据集业务键 */
    private String downstreamDatasetKey;

    /** CDP数仓血缘边转换类型 */
    private String transformType;

    /** CDP数仓血缘边转换引用 */
    private String transformRef;

    /** CDP数仓血缘边依赖类型 */
    private String dependencyType;

    /** CDP数仓血缘边说明 */
    private String description;

    /** CDP数仓血缘边是否生效 */
    private Boolean active;

    /** CDP数仓血缘边创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓血缘边最后更新时间 */
    private LocalDateTime updatedAt;
}
