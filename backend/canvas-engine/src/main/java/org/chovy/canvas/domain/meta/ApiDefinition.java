package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * API 接口定义（api_definition）。
 *
 * <p>运营在此注册可供 API_CALL 节点调用的内部接口，
 * 包含接口 URL、请求/响应字段 Schema 等信息。
 * API_CALL 节点通过 apiKey 引用对应接口定义，配置面板根据
 * requestSchema 动态生成入参表单。
 */
@Data
@TableName("api_definition")
public class ApiDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接口显示名称 */
    private String name;

    /** 接口唯一标识，API_CALL 节点配置时通过此 key 选择接口 */
    private String apiKey;

    /** 接口 URL（支持 {param} 路径变量） */
    private String url;

    /** HTTP 方法，如 GET、POST */
    private String method;

    /** 业务线标识，用于分组展示 */
    private String bizLine;

    /**
     * 请求参数 Schema，JSON 数组，格式：
     * {@code [{"name":"userId","displayName":"用户ID","type":"STRING","required":true}]}
     * 前端在 API_CALL 节点配置面板根据此 schema 渲染入参映射表单。
     */
    private String requestSchema;

    /**
     * 响应字段 Schema，JSON 数组，格式同 requestSchema。
     * 定义接口返回的字段，执行后写入上下文供后续节点引用。
     */
    private String responseSchema;

    /** 是否携带旅程环境信息，1=携带，0=不携带 */
    private Integer includeContextPayload;

    /** 是否开启回执等待，1=开启，0=不开启 */
    private Integer receiptEnabled;

    /** 回执等待过期时间（分钟） */
    private Integer receiptExpireMinutes;

    /** 视为回执完成的状态列表 JSON */
    private String receiptStatuses;

    /** 接口描述 */
    private String description;

    /** 每秒最大调用次数，null 表示不限制 */
    private Integer rateLimitPerSec;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
