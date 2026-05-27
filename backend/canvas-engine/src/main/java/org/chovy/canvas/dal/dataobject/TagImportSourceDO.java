package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签导入来源 数据对象，对应数据库表 {@code tag_import_source}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("tag_import_source")
public class TagImportSourceDO {

    @TableId(type = IdType.AUTO)
    /** 标签导入来源主键 ID */
    private Long id;

    /** 来源名称，供运营配置页选择 */
    private String name;

    /** 远程拉取 URL */
    private String url;

    /** HTTP 请求方法，如 GET、POST */
    private String method;

    /** 请求头 JSON */
    private String headersJson;

    /** 请求体模板，支持按实现约定填充分页参数 */
    private String bodyTemplate;

    /** 页码参数名 */
    private String pageParam;

    /** 每页大小参数名 */
    private String pageSizeParam;

    /** 每页拉取数量 */
    private Integer pageSize;

    /** 响应中记录列表的 JSONPath 或点路径 */
    private String recordsPath;

    /** 远程字段到标签导入字段的映射 JSON */
    private String fieldMapping;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 来源配置创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 来源配置最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
