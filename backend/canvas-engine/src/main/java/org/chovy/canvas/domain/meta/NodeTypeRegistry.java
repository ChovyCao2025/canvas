package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("node_type_registry")
public class NodeTypeRegistry {

    @TableId
    private String typeKey;

    private String typeName;
    private String category;
    private String handlerClass;

    /** 前端表单 Schema（JSON 数组字符串） */
    private String configSchema;

    /** 节点产出的上下文字段定义（JSON 数组字符串） */
    private String outputSchema;

    /** 1=触发器节点（无入边） */
    private Integer isTrigger;

    /** 1=终止节点（无出边） */
    private Integer isTerminal;

    private String description;
    private Integer enabled;
}
