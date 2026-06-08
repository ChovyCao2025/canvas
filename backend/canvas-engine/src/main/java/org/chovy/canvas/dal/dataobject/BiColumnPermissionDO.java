package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiColumnPermissionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_column_permission")
public class BiColumnPermissionDO {

    /** BI列权限主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI列权限字段业务键 */
    private String fieldKey;

    /** BI列权限主体类型 */
    private String subjectType;

    /** 关联的主体 ID */
    private String subjectId;

    /** BI列权限策略 */
    private String policy;

    /** BI列权限脱敏明细 JSON */
    private String maskJson;

    /** BI列权限是否启用 */
    private Boolean enabled;

    /** BI列权限创建时间 */
    private LocalDateTime createdAt;
}
