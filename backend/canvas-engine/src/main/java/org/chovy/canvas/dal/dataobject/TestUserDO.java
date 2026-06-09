package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TestUserDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("test_user")
public class TestUserDO {
    /** 测试用户主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的集合 ID */
    private Long setId;
    /** 关联的用户 ID */
    private String userId;
    /** 测试用户展示名称 */
    private String displayName;
    /** 测试用户画像明细 JSON */
    private String profileJson;
    /** 测试用户输入参数 */
    private String inputParams;
    /** 测试用户创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 测试用户最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
