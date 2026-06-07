package org.chovy.canvas.domain.bi.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class BiQueryGovernanceConfiguration {

    @Bean
    public BiQueryGovernancePolicy biQueryGovernancePolicy(
            @Value("${canvas.bi.query.timeout-ms:30000}") long defaultTimeoutMs,
            @Value("${canvas.bi.query.quota-rows:1000000}") int defaultQuotaRows) {
        return new BiQueryGovernancePolicy(defaultTimeoutMs, defaultQuotaRows, Map.of());
    }
}
