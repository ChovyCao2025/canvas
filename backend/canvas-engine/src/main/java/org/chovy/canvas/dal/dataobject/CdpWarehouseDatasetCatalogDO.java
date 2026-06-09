package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseDatasetCatalogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_dataset_catalog")
public class CdpWarehouseDatasetCatalogDO {

    /** CDP数仓数据集目录主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓数据集目录数据集业务键 */
    private String datasetKey;

    /** CDP数仓数据集目录LAYER */
    private String layer;

    /** CDP数仓数据集目录物理名称 */
    private String physicalName;

    /** CDP数仓数据集目录展示名称 */
    private String displayName;

    /** CDP数仓数据集目录主题域 */
    private String subjectArea;

    /** CDP数仓数据集目录来源系统 */
    private String sourceSystem;

    /** CDP数仓数据集目录负责人姓名 */
    private String ownerName;

    /** CDP数仓数据集目录说明 */
    private String description;

    /** CDP数仓数据集目录新鲜度 SLA 分钟数 */
    private Integer freshnessSlaMinutes;

    /** CDP数仓数据集目录PII 等级 */
    private String piiLevel;

    /** CDP数仓数据集目录当前状态 */
    private String status;

    /** CDP数仓数据集目录结构定义 JSON */
    private String schemaJson;

    /** CDP数仓数据集目录创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓数据集目录最后更新时间 */
    private LocalDateTime updatedAt;
}
