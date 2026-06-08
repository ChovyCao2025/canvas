package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasetVersionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_dataset_version")
public class BiDatasetVersionDO {

    /** BI数据集版本主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI数据集版本数据集业务键 */
    private String datasetKey;

    /** BI数据集版本版本号 */
    private Integer version;

    /** BI数据集版本当前状态 */
    private String status;

    /** BI数据集版本资源内容 JSON */
    private String resourceJson;

    /** BI数据集版本发布人 */
    private String publishedBy;

    /** BI数据集版本创建时间 */
    private LocalDateTime createdAt;
}
