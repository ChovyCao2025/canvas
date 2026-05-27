package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签导入批次 数据对象，对应数据库表 {@code tag_import_batch}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("tag_import_batch")
public class TagImportBatchDO {

    @TableId(type = IdType.AUTO)
    /** 标签导入批次主键 ID */
    private Long id;

    /** 导入来源类型，如 EXCEL、API、REMOTE_SOURCE */
    private String sourceType;

    /** 批次状态，如 PENDING、RUNNING、SUCCEEDED、FAILED */
    private String status;

    /** 导入文件名，文件导入场景使用 */
    private String fileName;

    /** 外部文件或远程来源 URL */
    private String externalUrl;

    /** 本批次总行数 */
    private Integer totalRows;

    /** 成功导入行数 */
    private Integer successRows;

    /** 导入失败行数 */
    private Integer failedRows;

    /** 导入发起人 */
    private String createdBy;

    /** 批次开始处理时间 */
    private LocalDateTime startedAt;

    /** 批次处理结束时间 */
    private LocalDateTime finishedAt;

    /** 批次级失败原因或错误摘要 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    /** 批次创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 批次最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
