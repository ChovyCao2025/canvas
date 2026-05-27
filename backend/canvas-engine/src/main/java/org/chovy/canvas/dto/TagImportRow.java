package org.chovy.canvas.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签导入 Row 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
@Data
public class TagImportRow {

    private Integer rowNo;
    private String idType;
    private String idValue;
    private String tagCode;
    private String tagValue;
    private LocalDateTime tagTime;
}
