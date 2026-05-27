package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 执行上下文字段定义（context_field）。
 *
 * <p>描述各节点类型执行后能写入 ExecutionContext 的字段，
 * 供前端在后续节点配置面板中提示可用的 {@code ${key}} 变量。
 * 目前为预留元数据表，运行时逻辑不依赖此表。
 */
@Data
@TableName("context_field")
public class ContextFieldDO {

    @TableId(type = IdType.AUTO)
    /** 上下文字段定义主键 ID */
    private Long id;

    /** 字段在上下文中的 key，对应 {@code ${fieldKey}} 表达式 */
    private String fieldKey;

    /** 字段显示名称，如"订单号"、"用户等级" */
    private String fieldName;

    /**
     * 字段数据类型。
     * 可选值：STRING、NUMBER、BOOLEAN、LIST
     */
    private String dataType;

    /** 产生此字段的节点类型，如 EVENT_TRIGGER、API_CALL，对应 NodeType 常量 */
    private String sourceNodeType;

    /** 字段描述 */
    private String description;
}
