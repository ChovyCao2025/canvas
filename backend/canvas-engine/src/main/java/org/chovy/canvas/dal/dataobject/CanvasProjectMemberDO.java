package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CanvasProjectMemberDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("canvas_project_member")
public class CanvasProjectMemberDO {
    /** 画布项目成员主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的项目 ID */
    private Long projectId;
    /** 关联的用户 ID */
    private Long userId;
    /** 画布项目成员用户名 */
    private String username;
    /** 画布项目成员角色 */
    private String role;
    /** 画布项目成员来源 */
    private String source;
    /** 画布项目成员创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 画布项目成员最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
