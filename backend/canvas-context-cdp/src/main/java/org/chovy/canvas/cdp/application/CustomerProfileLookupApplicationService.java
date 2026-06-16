package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpCustomerProfileView;
import org.chovy.canvas.cdp.api.CustomerProfileLookupPort;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * 编排 CustomerProfileLookup 的应用服务流程。
 */
@Service
public class CustomerProfileLookupApplicationService implements CustomerProfileLookupPort {

    /**
     * 仓储依赖。
     */
    private final CustomerProfileRepository repository;

    /**
     * 时间源。
     */
    private final Clock clock;

    /**
     * 创建当前组件实例。
     */
    @Autowired
    public CustomerProfileLookupApplicationService(CustomerProfileRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    CustomerProfileLookupApplicationService(CustomerProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行 ensureUser 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdpCustomerProfileView ensureUser(Long tenantId, String userId, String sourceType, String sourceRefId) {
        return toView(ensureUserProfile(tenantId, userId, sourceType, sourceRefId));
    }

    /**
     * 执行 ensureUserProfile 对应的 CDP 业务操作。
     */
    CustomerProfile ensureUserProfile(Long tenantId, String userId, String sourceType, String sourceRefId) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        LocalDateTime now = LocalDateTime.now(clock);
        CustomerProfile existing = repository.findProfile(scopedTenantId, normalizedUserId);
        if (existing != null) {
            LocalDateTime firstSeen = existing.firstSeenAt() == null ? now : existing.firstSeenAt();
            return repository.saveProfile(existing.withSeenAt(firstSeen, now));
        }
        CustomerProfile created = new CustomerProfile(
                null,
                scopedTenantId,
                normalizedUserId,
                normalizedUserId,
                null,
                null,
                "ACTIVE",
                Map.of(),
                now,
                now,
                defaultString(sourceType, "CDP"),
                null,
                null);
        CustomerProfile saved = repository.saveProfile(created);
        repository.saveIdentity(scopedTenantId, saved.userId(), "USER_ID", saved.userId(), sourceType, sourceRefId, true);
        return saved;
    }

    /**
     * 执行 ensureUserByIdentity 对应的 CDP 业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdpCustomerProfileView ensureUserByIdentity(Long tenantId, String identityType, String identityValue,
                                                       String sourceType, String sourceRefId) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedType = requireText(identityType, "identityType").toUpperCase(Locale.ROOT);
        String normalizedValue = requireText(identityValue, "identityValue");
        if ("USER_ID".equals(normalizedType)) {
            return ensureUser(scopedTenantId, normalizedValue, sourceType, sourceRefId);
        }

        String existingUserId = repository.findUserIdByIdentity(scopedTenantId, normalizedType, normalizedValue);
        if (existingUserId != null && !existingUserId.isBlank()) {
            return ensureUser(scopedTenantId, existingUserId, sourceType, sourceRefId);
        }

        String generatedUserId = normalizedType.toLowerCase(Locale.ROOT) + ":" + normalizedValue;
        CustomerProfile profile = ensureUserProfile(scopedTenantId, generatedUserId, sourceType, sourceRefId);
        repository.saveIdentity(scopedTenantId, profile.userId(), normalizedType, normalizedValue, sourceType,
                sourceRefId, false);
        return toView(profile);
    }

    /**
     * 返回required Profile。
     */
    @Override
    public CdpCustomerProfileView getRequiredProfile(Long tenantId, String userId) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedUserId = requireText(userId, "userId");
        CustomerProfile profile = repository.findProfile(scopedTenantId, normalizedUserId);
        if (profile == null) {
            throw new IllegalArgumentException("CDP user does not exist: " + normalizedUserId);
        }
        return toView(profile);
    }

    /**
     * 转换为View。
     */
    private CdpCustomerProfileView toView(CustomerProfile profile) {
        return new CdpCustomerProfileView(
                profile.id(),
                profile.tenantId(),
                profile.userId(),
                profile.displayName(),
                profile.phone(),
                profile.email(),
                profile.status(),
                profile.properties() == null ? Map.of() : profile.properties(),
                profile.firstSeenAt(),
                profile.lastSeenAt());
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 读取并校验必填的Text。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 返回默认的String。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
