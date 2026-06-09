package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TenantDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("tenant")
public class TenantDO {

    /** 租户主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户租户业务键 */
    private String tenantKey;
    /** 租户名称 */
    private String name;
    /** 租户当前状态 */
    private String status;
    /** 租户计划编码 */
    private String planCode;
    /** 租户配额明细 JSON */
    private String quotaJson;
    /** 租户备注 */
    private String remark;
    /** 租户创建人 */
    private String createdBy;

    /** 租户创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 租户最后更新人 */
    private String updatedBy;

    /** 租户最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
