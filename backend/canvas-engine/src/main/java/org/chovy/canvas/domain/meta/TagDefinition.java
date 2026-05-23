package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户标签定义（tag_definition）。
 *
 * <p>注册可在 TAGGER 节点中使用的标签，分为离线标签（offline）和实时标签（realtime）。
 * 执行引擎根据 tagCode 调用对应的 Tagger 服务为用户打标。
 */
@Data
@TableName("tag_definition")
public class TagDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签显示名称 */
    private String name;

    /** 标签唯一标识，TAGGER 节点通过此 code 选择要打的标签 */
    private String tagCode;

    /**
     * 标签类型。
     * <ul>
     *   <li>{@code offline} — 离线标签，由 TAGGER_OFFLINE 节点处理，异步写入标签平台</li>
     *   <li>{@code realtime} — 实时标签，由 TAGGER_REALTIME 节点处理，同步打标</li>
     * </ul>
     */
    private String tagType;

    /** 标签值类型：STRING / NUMBER / BOOLEAN */
    private String valueType;

    /** 标签描述 */
    private String description;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
