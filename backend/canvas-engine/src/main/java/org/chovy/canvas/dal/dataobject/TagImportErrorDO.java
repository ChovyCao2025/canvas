package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签导入错误 数据对象，对应数据库表 {@code tag_import_error}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("tag_import_error")
public class TagImportErrorDO {

    @TableId(type = IdType.AUTO)
    /** 标签导入错误主键 ID */
    private Long id;

    /** 所属导入批次 ID，对应 tag_import_batch.id */
    private Long batchId;

    /** 出错行号，从导入文件或远程结果中的行序号派生 */
    private Integer rowNo;

    /** 原始行数据 JSON，便于定位和修复导入问题 */
    private String rawPayload;

    /** 错误编码，用于前端分类展示 */
    private String errorCode;

    /** 错误详情 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    /** 错误记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;
}
