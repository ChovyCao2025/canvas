package org.chovy.canvas.domain.monitoring;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * EnvironmentMarketingMonitorProviderCredentialResolver 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class EnvironmentMarketingMonitorProviderCredentialResolver implements MarketingMonitorProviderCredentialResolver {

    private final ObjectProvider<MarketingMonitorProviderCredentialService> credentialServiceProvider;

    /**
     * 创建 EnvironmentMarketingMonitorProviderCredentialResolver 实例并注入 domain.monitoring 场景依赖。
     * @param credentialServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public EnvironmentMarketingMonitorProviderCredentialResolver(
            ObjectProvider<MarketingMonitorProviderCredentialService> credentialServiceProvider) {
        this.credentialServiceProvider = credentialServiceProvider;
    }

    /**
     * 从 JVM 系统属性或环境变量解析明文凭据。
     *
     * <p>reference 会被当作属性名或环境变量名使用；方法不访问数据库，也不做解密。
     * 空引用或未配置值返回 {@code null}。</p>
     *
     * @param reference 系统属性名或环境变量名
     * @return 解析到的明文凭据；未配置时返回 {@code null}
     */
    @Override
    public String resolve(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String key = reference.trim();
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String value = System.getenv(key);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 按租户解析凭据引用。
     *
     * <p>当引用以 {@code credential:} 开头时委托凭据服务读取并解密租户级托管凭据；其他引用退回到
     * {@link #resolve(String)} 从系统属性或环境变量解析。</p>
     *
     * @param tenantId 租户 ID，用于查询托管凭据
     * @param reference 凭据引用，支持 {@code credential:} 前缀或环境变量名
     * @return 明文凭据；服务不可用、引用为空或凭据不存在时返回 {@code null}
     */
    @Override
    public String resolve(Long tenantId, String reference) {
        if (reference != null && reference.trim().startsWith("credential:")) {
            MarketingMonitorProviderCredentialService service =
                    credentialServiceProvider == null ? null : credentialServiceProvider.getIfAvailable();
            if (service == null) {
                return null;
            }
            return service.resolveValue(tenantId, reference);
        }
        return resolve(reference);
    }
}
