package org.chovy.canvas.dto.datasource;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;

/** Request DTO for creating or updating managed data source configurations. */
public record DataSourceConfigReq(
        @Positive
        Long tenantId,
        @NotBlank
        @Size(max = 128)
        String name,
        @Pattern(regexp = "JDBC")
        String type,
        @NotBlank
        @Size(max = 1024)
        String url,
        @NotBlank
        @Size(max = 256)
        String username,
        @NotBlank
        @Size(max = 2048)
        String password,
        @Size(max = 256)
        String driverClassName,
        @Size(max = 1024)
        String description,
        @Min(0)
        @Max(1)
        Integer enabled,
        @Size(max = 128)
        String createdBy
) {
    /**
     * 组装输出结构或完成对象转换。
     *
     * @return 返回组装或转换后的结果对象。
     */
    public DataSourceConfigDO toDataObject() {
        DataSourceConfigDO body = new DataSourceConfigDO();
        body.setTenantId(tenantId);
        body.setName(name);
        body.setType(type);
        body.setUrl(url);
        body.setUsername(username);
        body.setPassword(password);
        body.setDriverClassName(driverClassName);
        body.setDescription(description);
        body.setEnabled(enabled);
        body.setCreatedBy(createdBy);
        return body;
    }
}
