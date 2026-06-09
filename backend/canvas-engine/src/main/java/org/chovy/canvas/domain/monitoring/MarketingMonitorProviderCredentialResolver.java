package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderCredentialResolver 定义 domain.monitoring 场景中的扩展契约。
 */
@FunctionalInterface
public interface MarketingMonitorProviderCredentialResolver {

    /**
     * 解析业务依赖或上下文值。
     *
     * @param reference reference 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @return 返回 resolve 生成的文本或业务键。
     */
    String resolve(String reference);

    /**
     * 解析业务依赖或上下文值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param reference reference 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @return 返回 resolve 生成的文本或业务键。
     */
    default String resolve(Long tenantId, String reference) {
        return resolve(reference);
    }
}
