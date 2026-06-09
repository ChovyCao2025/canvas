package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统选项 数据对象，对应数据库表 {@code system_option}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("system_option")
public class SystemOptionDO {

    @TableId(type = IdType.AUTO)
    /** 系统选项主键 ID */
    private Long id;

    /** 节点执行耗时（毫秒） */
    @TableField("tenant_id")
    private Long tenantId;

    /** 选项分类，用于前端按业务域分组展示 */
    private String category;

    /** 选项唯一键，同一 category 下不可重复 */
    private String optionKey;

    /** 选项展示名称 */
    private String label;

    /** 选项说明，展示给运营或管理员理解用途 */
    private String description;

    /** 展示排序，值越小越靠前 */
    private Integer sortOrder;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 是否系统内置选项，1=内置不可随意删除，0=业务自定义 */
    private Integer systemBuiltin;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
