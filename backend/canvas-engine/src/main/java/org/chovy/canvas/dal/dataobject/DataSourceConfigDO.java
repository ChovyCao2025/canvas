package org.chovy.canvas.dal.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 平台级数据源配置。 */
@Data
@TableName("data_source_config")
public class DataSourceConfigDO {

    @TableId(type = IdType.AUTO)
    /** 数据源配置主键 ID */
    private Long id;

    /** 配置名称，供业务配置页下拉选择。 */
    private String name;

    /** 数据源类型，当前支持 JDBC。 */
    private String type;

    /** JDBC URL。 */
    private String url;

    /** JDBC 用户名。 */
    private String username;

    /** JDBC 密码。 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** JDBC 驱动类。 */
    private String driverClassName;

    /** 配置说明。 */
    private String description;

    /** 启用状态：1=启用，0=禁用。 */
    private Integer enabled;

    /** 创建人。 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
