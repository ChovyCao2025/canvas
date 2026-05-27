package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDP 用户标签 数据对象，对应数据库表 {@code cdp_user_tag}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("cdp_user_tag")
public class CdpUserTagDO {

    @TableId(type = IdType.AUTO)
    /** 用户标签记录主键 ID */
    private Long id;

    /** CDP 内部统一用户 ID */
    private String userId;

    /** 标签编码，对应 tag_definition.tag_code */
    private String tagCode;

    /** 当前生效的标签值 */
    private String tagValue;

    /** 标签值类型，如 STRING、NUMBER、BOOLEAN、JSON */
    private String valueType;

    /** 标签来源类型，如 CANVAS、IMPORT、MANUAL */
    private String sourceType;

    /** 来源引用 ID，如执行 ID、导入批次 ID 或操作记录 ID */
    private String sourceRefId;

    /** 标签状态，如 ACTIVE、EXPIRED、DELETED */
    private String status;

    /** 标签生效时间 */
    private LocalDateTime effectiveAt;

    /** 标签过期时间，null 表示长期有效 */
    private LocalDateTime expiresAt;

    /** 创建人或写入来源 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
