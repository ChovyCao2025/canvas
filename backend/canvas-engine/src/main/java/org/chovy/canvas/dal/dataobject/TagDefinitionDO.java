package org.chovy.canvas.dal.dataobject;

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
public class TagDefinitionDO {

    @TableId(type = IdType.AUTO)
    /** 标签定义主键 ID */
    private Long id;

    /** 标签显示名称 */
    private String name;

    /** 标签唯一标识，TAGGER 节点通过此 code 选择要打的标签 */
    private String tagCode;

    /**
     * 标签类型。
     * <ul>
     *   <li>{@code offline} — 离线标签，由 TAGGER 节点异步写入标签平台</li>
     *   <li>{@code realtime} — 实时标签，由 TAGGER 节点同步打标</li>
     * </ul>
     */
    private String tagType;

    /** 标签描述 */
    private String description;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 标签值类型：STRING / NUMBER / BOOLEAN / JSON */
    private String valueType;

    /** 是否允许人工打标，1=允许，0=不允许 */
    private Integer manualEnabled;

    /** 默认有效期天数，null=长期有效 */
    private Integer defaultTtlDays;

    /** 标签分类 */
    private String category;

    /** 负责人 */
    private String owner;

    /** 第一批仅启用 UPSERT，APPEND 为后续多值标签预留 */
    private String writePolicy;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
