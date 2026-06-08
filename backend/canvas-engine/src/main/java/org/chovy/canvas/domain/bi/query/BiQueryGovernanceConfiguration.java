package org.chovy.canvas.domain.bi.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * BiQueryGovernanceConfiguration 编排 domain.bi.query 场景的领域业务规则。
 */
@Configuration
public class BiQueryGovernanceConfiguration {

    /**
     * 创建 BI 查询治理策略 Bean。
     *
     * @param defaultTimeoutMs 默认查询超时毫秒数，来自 {@code canvas.bi.query.timeout-ms}
     * @param defaultQuotaRows 默认查询行数配额，来自 {@code canvas.bi.query.quota-rows}
     * @return 只包含默认口径的治理策略，数据集级覆盖由服务侧配置接口维护
     */
    @Bean
    public BiQueryGovernancePolicy biQueryGovernancePolicy(
            @Value("${canvas.bi.query.timeout-ms:30000}") long defaultTimeoutMs,
            @Value("${canvas.bi.query.quota-rows:1000000}") int defaultQuotaRows) {
        return new BiQueryGovernancePolicy(defaultTimeoutMs, defaultQuotaRows, Map.of());
    }
}
