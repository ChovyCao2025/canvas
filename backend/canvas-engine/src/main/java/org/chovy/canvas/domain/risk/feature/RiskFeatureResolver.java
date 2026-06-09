package org.chovy.canvas.domain.risk.feature;

import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest;
import org.chovy.canvas.domain.risk.runtime.RiskRequestFeatureResolver;
import org.chovy.canvas.domain.risk.runtime.RiskResolvedValue;
import org.chovy.canvas.domain.risk.runtime.RiskSubjectHasher;
import org.chovy.canvas.domain.risk.runtime.RiskSubjectHashing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线风控特征解析器，按请求覆盖、本地缓存和特征存储的顺序解析 FEATURE 操作数。
 */
public class RiskFeatureResolver implements RiskRequestFeatureResolver {

    private final RiskFeatureCatalogService catalog;
    private final RiskFeatureStore featureStore;
    private final Duration cacheTtl;
    private final RiskSubjectHasher hasher;
    private final Map<String, CachedFeature> cache = new ConcurrentHashMap<>();

    /**
     * 使用默认主体哈希器创建特征解析器。
     */
    public RiskFeatureResolver(RiskFeatureCatalogService catalog, RiskFeatureStore featureStore, Duration cacheTtl) {
        this(catalog, featureStore, cacheTtl, RiskSubjectHashing::sha256);
    }

    /**
     * 使用指定哈希器创建特征解析器，并规范化缓存过期时间。
     */
    public RiskFeatureResolver(RiskFeatureCatalogService catalog,
                               RiskFeatureStore featureStore,
                               Duration cacheTtl,
                               RiskSubjectHasher hasher) {
        this.catalog = catalog;
        this.featureStore = featureStore;
        this.cacheTtl = cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero() ? Duration.ofMinutes(5) : cacheTtl;
        this.hasher = hasher == null ? RiskSubjectHashing::sha256 : hasher;
    }

    /**
     * 解析规则中的 FEATURE 操作数，返回存在值或缺失标记。
     */
    @Override
    public RiskResolvedValue resolve(RiskDecisionRequest request, RiskRuleOperand operand) {
        if (request == null || !(operand instanceof RiskRuleOperand.FeatureOperand featureOperand)) {
            return RiskResolvedValue.missing();
        }
        String featureKey = featureOperand.key();
        if (request.suppliedFeatures() != null && request.suppliedFeatures().containsKey(featureKey)) {
            // 请求内显式特征优先，便于仿真和可信调用方固定特征快照。
            return RiskResolvedValue.present(request.suppliedFeatures().get(featureKey));
        }
        Optional<String> subjectField = catalog.subjectFieldFor(featureKey);
        if (subjectField.isEmpty()) {
            return RiskResolvedValue.missing();
        }
        Object rawSubject = request.subject() == null ? null : request.subject().get(subjectField.get());
        if (rawSubject == null) {
            return RiskResolvedValue.missing();
        }
        // 特征存储按目录指定的主体字段查找，而不是使用完整主体载荷。
        String subjectHash = hasher.hash(rawSubject.toString());
        String cacheKey = request.tenantId() + ":" + featureKey + ":" + subjectHash;
        CachedFeature cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return RiskResolvedValue.present(cached.value());
        }
        Optional<Object> value = featureStore.get(request.tenantId(), featureKey, subjectHash);
        value.ifPresent(item -> cache.put(cacheKey, new CachedFeature(item, Instant.now().plus(cacheTtl))));
        return value.map(RiskResolvedValue::present).orElseGet(RiskResolvedValue::missing);
    }

    /**
     * 本地缓存中的特征值及过期时间。
     */
    private record CachedFeature(Object value, Instant expiresAt) {

        /**
         * 判断缓存值是否已过期。
         */
        private boolean isExpired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
