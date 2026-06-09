package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
/**
 * ChannelConnectorRegistry 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class ChannelConnectorRegistry {

    private final Repository repository;
    private final Map<String, ChannelConnector> realConnectors;

    /**
     * 初始化 ChannelConnectorRegistry 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     * @param MapString map string 参数，用于 ChannelConnectorRegistry 流程中的校验、计算或对象转换。
     * @param realConnectors real connectors 参数，用于 ChannelConnectorRegistry 流程中的校验、计算或对象转换。
     */
    public ChannelConnectorRegistry(Repository repository, Map<String, ChannelConnector> realConnectors) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.realConnectors = realConnectors == null ? Map.of() : realConnectors;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @return 返回 resolve 流程生成的业务结果。
     */
    public ChannelConnector resolve(Long tenantId, String channel, String provider) {
        String normalizedChannel = normalize(channel);
        String normalizedProvider = normalizeProvider(provider);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConnectorConfig config = repository.find(tenant(tenantId), normalizedChannel, normalizedProvider);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (config == null) {
            return new DisabledChannelConnector(normalizedChannel + "/" + normalizedProvider + " connector not configured");
        }
        if (config.mode() == ChannelConnector.ConnectorMode.DISABLED) {
            return new DisabledChannelConnector(config.disabledReason());
        }
        if (config.mode() == ChannelConnector.ConnectorMode.SANDBOX) {
            return new SandboxConnector(config);
        }
        ChannelConnector connector = realConnectors.get(config.connectorKey());
        if (connector == null) {
            connector = realConnectors.get(config.channel() + ":" + config.provider());
        }
        if (connector == null) {
            return new DisabledChannelConnector("real connector not registered: " + config.connectorKey());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return connector;
    }

    /**
     * Repository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface Repository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param channel channel 参数，用于 find 流程中的校验、计算或对象转换。
         * @param provider provider 参数，用于 find 流程中的校验、计算或对象转换。
         * @return 返回符合条件的数据列表或视图。
         */
        ConnectorConfig find(Long tenantId, String channel, String provider);
    }

    /**
     * ConnectorConfig 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record ConnectorConfig(
            String connectorKey,
            String channel,
            String provider,
            ChannelConnector.ConnectorMode mode,
            String disabledReason,
            String healthStatus,
            String healthMessage,
            String capabilitiesJson) {

        /**
         * 初始化 ConnectorConfig 实例。
         *
         * @param connectorKey 业务键，用于在同一租户下定位资源。
         * @param channel channel 参数，用于 ConnectorConfig 流程中的校验、计算或对象转换。
         * @param provider provider 参数，用于 ConnectorConfig 流程中的校验、计算或对象转换。
         * @param mode mode 参数，用于 ConnectorConfig 流程中的校验、计算或对象转换。
         * @param disabledReason 原因说明，用于记录状态变化的业务依据。
         */
        public ConnectorConfig(String connectorKey,
                               String channel,
                               String provider,
                               ChannelConnector.ConnectorMode mode,
                               String disabledReason) {
            this(connectorKey, channel, provider, mode, disabledReason, "UNKNOWN", null, null);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static ChannelConnector.ConnectorMode parseMode(String value) {
        if (value == null || value.isBlank()) {
            return ChannelConnector.ConnectorMode.DISABLED;
        }
        try {
            return ChannelConnector.ConnectorMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChannelConnector.ConnectorMode.DISABLED;
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalize(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeProvider(String value) {
        return value == null || value.isBlank() ? "DEFAULT" : normalize(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant 计算得到的数量、金额或指标值。
     */
    static Long tenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * SandboxConnector 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    private static class SandboxConnector implements ChannelConnector {

        private final ConnectorConfig config;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param config 配置对象，用于控制运行参数和策略开关。
         * @return 返回 SandboxConnector 流程生成的业务结果。
         */
        private SandboxConnector(ConnectorConfig config) {
            this.config = config;
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 mode 流程生成的业务结果。
         */
        public ConnectorMode mode() {
            return ConnectorMode.SANDBOX;
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 health 流程生成的业务结果。
         */
        public ConnectorHealth health() {
            return new ConnectorHealth(
                    config.healthStatus() == null ? "UP" : config.healthStatus(),
                    config.healthMessage() == null ? "sandbox connector ready" : config.healthMessage());
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 capabilities 流程生成的业务结果。
         */
        public ConnectorCapabilities capabilities() {
            return new ConnectorCapabilities(true, false, Map.of());
        }

        @Override
        /**
         * 执行核心业务流程，并协调依赖组件完成处理。
         *
         * @param request 请求对象，承载本次操作的输入参数。
         * @return 返回 send 流程生成的业务结果。
         */
        public ConnectorSendResult send(ConnectorSendRequest request) {
            String userId = request == null || request.userId() == null ? "anonymous" : request.userId();
            return new ConnectorSendResult(
                    true,
                    "sandbox-" + config.channel() + "-" + userId,
                    "ACCEPTED",
                    null);
        }

        @Override
        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param MapString map string 参数，用于 parseReceipt 流程中的校验、计算或对象转换。
         * @param rawPayload raw payload 参数，用于 parseReceipt 流程中的校验、计算或对象转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
            return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
        }
    }
}
