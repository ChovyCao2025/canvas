package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签值定义 数据对象，对应数据库表 {@code tag_value_definition}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("tag_value_definition")
public class TagValueDefinitionDO {

    @TableId(type = IdType.AUTO)
    /** 标签值定义主键 ID */
    private Long id;

    /** 所属标签编码，对应 tag_definition.tag_code */
    private String tagCode;

    /** 标签实际存储值 */
    private String value;

    /** 标签值展示名称 */
    private String label;

    /** 展示排序，值越小越靠前 */
    private Integer sortOrder;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 标签值来源，如 SYSTEM、IMPORT、MANUAL */
    private String source;

    /** 标签值说明 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
