package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AudienceBitmapRollbackDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("audience_bitmap_rollback")
public class AudienceBitmapRollbackDO {

    /** 人群位图回滚主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 人群位图回滚目标版本 */
    private Long targetVersion;

    /** 人群位图回滚目标位图业务键 */
    private String targetBitmapKey;

    /** 人群位图回滚回滚影响的版本数 */
    private Long rolledBackVersions;

    /** 人群位图回滚当前状态 */
    private String status;

    /** 人群位图回滚原因说明 */
    private String reason;

    /** 人群位图回滚操作员 */
    @TableField("operator_name")
    private String operator;

    /** 人群位图回滚创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
