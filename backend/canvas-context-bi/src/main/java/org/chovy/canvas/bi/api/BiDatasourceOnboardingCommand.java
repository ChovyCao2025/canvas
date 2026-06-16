package org.chovy.canvas.bi.api;

import java.util.Map;
/**
 * BiDatasourceOnboardingCommand 命令。
 */
public record BiDatasourceOnboardingCommand(
        /**
         * connectorType 字段值。
         */
        String connectorType,
        /**
         * 展示名称。
         */
        String name,
        /**
         * url 字段值。
         */
        String url,
        /**
         * username 字段值。
         */
        String username,
        /**
         * 凭据密码。
         */
        String password,
        /**
         * sourceKey 对应的业务键。
         */
        String sourceKey,
        /**
         * 说明文本。
         */
        String description,
        /**
         * enabled 字段值。
         */
        Boolean enabled,
        /**
         * 状态值。
         */
        String status,
        Map<String, Object> connectorConfig) {

    public BiDatasourceOnboardingCommand {
        connectorConfig = connectorConfig == null ? Map.of() : Map.copyOf(connectorConfig);
    }
}
