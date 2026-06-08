package org.chovy.canvas.domain.bi.datasource;

import java.util.Map;

/**
 * BiDatasourceOnboardingCommand 承载 domain.bi.datasource 场景中的不可变数据快照。
 * @param connectorType connectorType 字段。
 * @param name name 字段。
 * @param url url 字段。
 * @param username username 字段。
 * @param password password 字段。
 * @param driverClassName driverClassName 字段。
 * @param description description 字段。
 * @param enabled enabled 字段。
 * @param connectionMode connectionMode 字段。
 * @param connectorConfig connectorConfig 字段。
 */
public record BiDatasourceOnboardingCommand(
        String connectorType,
        String name,
        String url,
        String username,
        String password,
        String driverClassName,
        String description,
        Boolean enabled,
        String connectionMode,
        Map<String, Object> connectorConfig) {

    /**
     * 创建 BiDatasourceOnboardingCommand 实例并注入 domain.bi.datasource 场景依赖。
     * @param connectorType 类型标识，用于选择对应处理分支。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param url url 参数，用于 BiDatasourceOnboardingCommand 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 BiDatasourceOnboardingCommand 流程中的校验、计算或对象转换。
     * @param driverClassName 名称文本，用于展示或唯一性校验。
     * @param description 说明文本，用于补充业务上下文。
     * @param enabled enabled 参数，用于 BiDatasourceOnboardingCommand 流程中的校验、计算或对象转换。
     * @param connectionMode connection mode 参数，用于 BiDatasourceOnboardingCommand 流程中的校验、计算或对象转换。
     */
    public BiDatasourceOnboardingCommand(String connectorType,
                                         String name,
                                         String url,
                                         String username,
                                         String password,
                                         String driverClassName,
                                         String description,
                                         Boolean enabled,
                                         String connectionMode) {
        this(connectorType, name, url, username, password, driverClassName, description, enabled, connectionMode, Map.of());
    }

    public BiDatasourceOnboardingCommand {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
