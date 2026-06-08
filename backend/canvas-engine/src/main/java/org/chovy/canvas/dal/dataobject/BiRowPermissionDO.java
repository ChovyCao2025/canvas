package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiRowPermissionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_row_permission")
public class BiRowPermissionDO {

    /** BI行权限主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI行权限规则业务键 */
    private String ruleKey;

    /** BI行权限主体类型 */
    private String subjectType;

    /** 关联的主体 ID */
    private String subjectId;

    /** BI行权限筛选明细 JSON */
    private String filterJson;

    /** BI行权限是否启用 */
    private Boolean enabled;

    /** BI行权限创建时间 */
    private LocalDateTime createdAt;
}
