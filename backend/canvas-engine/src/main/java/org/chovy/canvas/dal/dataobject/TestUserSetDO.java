package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TestUserSetDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("test_user_set")
public class TestUserSetDO {
    /** 测试用户集合主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 测试用户集合名称 */
    private String name;
    /** 测试用户集合说明 */
    private String description;
    /** 测试用户集合创建人 */
    private String createdBy;
    /** 测试用户集合创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 测试用户集合最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
