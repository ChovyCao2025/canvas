package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业务事件定义（event_definition）。
 *
 * <p>运营在此注册业务事件（如"订单完成"），定义事件标识（eventCode）和上报属性字段。
 * EVENT_TRIGGER 节点通过 eventCode 订阅对应事件，
 * {@code /canvas/events/report} 接口接收业务方上报后触发匹配的画布。
 */
@Data
@TableName("event_definition")
public class EventDefinitionDO {

    @TableId(type = IdType.AUTO)
    /** 事件定义主键 ID */
    private Long id;

    /** 事件显示名称，如"订单完成" */
    private String name;

    /** 事件唯一标识，业务方上报时传入，如 ORDER_COMPLETE */
    private String eventCode;

    /**
     * 事件属性字段定义，JSON 数组，格式：
     * {@code [{"name":"orderId","displayName":"订单号","type":"STRING","required":true}]}
     * 前端用于在 EVENT_TRIGGER 节点配置面板展示可用上下文变量。
     */
    private String attributes;

    /** 事件描述 */
    private String description;

    /** 是否允许 SDK 上报自动发现新属性：1=允许，0=拒绝。 */
    private Integer autoDiscover;

    /** 自动发现策略：REJECT_UNKNOWN 或 PENDING_REVIEW。 */
    private String discoveryMode;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
