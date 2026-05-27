package org.chovy.canvas.dto;

import lombok.Data;

/**
 * 标签导入 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
@Data
public class TagImportResult {

    /** 标签导入批次 ID，对应 tag_import_batch.id。 */
    private Long batchId;

    /** 批次最终状态：SUCCESS、FAILED 或 PARTIAL_SUCCESS。 */
    private String status;

    /** 本次导入提交的总行数。 */
    private Integer totalRows;

    /** 校验通过并成功写入的行数。 */
    private Integer successRows;

    /** 校验失败或写入失败的行数。 */
    private Integer failedRows;
}
