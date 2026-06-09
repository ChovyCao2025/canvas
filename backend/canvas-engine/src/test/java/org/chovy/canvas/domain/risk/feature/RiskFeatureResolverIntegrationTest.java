package org.chovy.canvas.domain.risk.feature;

import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest;
import org.chovy.canvas.domain.risk.runtime.RiskResolvedValue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiskFeatureResolverIntegrationTest {

    private final FakeFeatureStore featureStore = new FakeFeatureStore();
    private final RiskFeatureCatalogService catalog = new RiskFeatureCatalogService();
    private final RiskFeatureResolver resolver = new RiskFeatureResolver(catalog, featureStore, Duration.ofMinutes(5));

    @Test
    void requestPayloadWinsBeforeCacheAndRedis() {
        featureStore.values.put(key("user.fail_count_1d"), 99);

        RiskResolvedValue value = resolver.resolve(request(Map.of("user.fail_count_1d", 3)),
                RiskRuleOperand.feature("user.fail_count_1d"));

        assertThat(value).isEqualTo(RiskResolvedValue.present(3));
        assertThat(featureStore.reads).isEmpty();
    }

    @Test
    void memoryCacheWinsBeforeRedisAfterFirstLookup() {
        featureStore.values.put(key("user.fail_count_1d"), 4);
        RiskDecisionRequest request = request(Map.of());

        assertThat(resolver.resolve(request, RiskRuleOperand.feature("user.fail_count_1d")))
                .isEqualTo(RiskResolvedValue.present(4));
        featureStore.values.put(key("user.fail_count_1d"), 9);
        assertThat(resolver.resolve(request, RiskRuleOperand.feature("user.fail_count_1d")))
                .isEqualTo(RiskResolvedValue.present(4));
        assertThat(featureStore.reads).containsExactly(key("user.fail_count_1d"));
    }

    @Test
    void redisFallbackUsesTenantFeatureKeyAndSubjectHash() {
        featureStore.values.put(key("user.segment"), "vip");

        RiskResolvedValue value = resolver.resolve(request(Map.of()), RiskRuleOperand.feature("user.segment"));

        assertThat(value).isEqualTo(RiskResolvedValue.present("vip"));
        assertThat(featureStore.reads).containsExactly(key("user.segment"));
    }

    @Test
    void missingFeatureReturnsMissing() {
        assertThat(resolver.resolve(request(Map.of()), RiskRuleOperand.feature("missing.feature")))
                .isEqualTo(RiskResolvedValue.missing());
    }

    private RiskDecisionRequest request(Map<String, Object> features) {
        return new RiskDecisionRequest(
                7L,
                "req-1",
                "MARKETING_BENEFIT_ISSUE",
                Instant.parse("2026-06-08T10:00:00Z"),
                Map.of(),
                Map.of("userId", "user-1"),
                Map.of(),
                features,
                50);
    }

    private String key(String featureKey) {
        return "7:" + featureKey + ":" + subjectHash("user-1");
    }

    private String subjectHash(String rawSubject) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(rawSubject.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class FakeFeatureStore implements RiskFeatureStore {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final java.util.List<String> reads = new java.util.ArrayList<>();

        @Override
        public void set(Long tenantId, String featureKey, String subjectHash, Object value, Duration ttl) {
            values.put(key(tenantId, featureKey, subjectHash), value);
        }

        @Override
        public Optional<Object> get(Long tenantId, String featureKey, String subjectHash) {
            String key = key(tenantId, featureKey, subjectHash);
            reads.add(key);
            return Optional.ofNullable(values.get(key));
        }

        private String key(Long tenantId, String featureKey, String subjectHash) {
            return tenantId + ":" + featureKey + ":" + subjectHash;
        }
    }
}
