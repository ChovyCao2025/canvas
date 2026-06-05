package org.chovy.canvas.query;

import lombok.Data;

/**
 * 画布列表查询参数。
 *
 * <p>用于后台分页查询接口。
 * 该对象仅表达“查询条件”，不包含排序白名单等安全策略（由服务层控制）。
 */
@Data
public class CanvasListQuery {

    /** 页码（从 1 开始，非法值建议在 Controller 层兜底修正）。 */
    private int page = 1;

    /** 每页条数（默认 20，建议上层限制最大值防止大页查询）。 */
    private int size = 20;

    /** 画布状态：0草稿 1已发布 2已下线；null 表示不过滤。 */
    private Integer status;

    /** 按名称模糊匹配（可选，忽略大小写由数据库规则决定）。 */
    private String name;

    /** 所属租户 ID，由认证上下文写入；超级管理员可显式指定或留空查全部。 */
    private Long tenantId;

    /** 平铺项目分组 key，精确匹配。 */
    private String projectKey;

    /** 平铺文件夹分组 key，精确匹配。 */
    private String folderKey;
}
