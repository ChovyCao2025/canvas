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

    /** 原始数据行号，用于错误明细定位。 */
    private Integer rowNo;

    /** 用户身份类型，如 USER_ID、EMAIL 或 PHONE。 */
    private String idType;

    /** 用户身份值，与 idType 配合定位或创建 CDP 用户。 */
    private String idValue;

    /** 标签编码，对应 tag_definition.tag_code。 */
    private String tagCode;

    /** 标签值，会按标签定义的 valueType 做校验和规范化。 */
    private String tagValue;

    /** 标签发生或采集时间，可为空。 */
    private LocalDateTime tagTime;
}
