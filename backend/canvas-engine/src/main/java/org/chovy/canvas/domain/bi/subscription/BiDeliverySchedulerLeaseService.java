package org.chovy.canvas.domain.bi.subscription;

import org.chovy.canvas.dal.dataobject.BiDeliverySchedulerLeaseDO;
import org.chovy.canvas.dal.mapper.BiDeliverySchedulerLeaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BiDeliverySchedulerLeaseService {

    private final BiDeliverySchedulerLeaseMapper leaseMapper;
    private final String ownerId;

    public BiDeliverySchedulerLeaseService(BiDeliverySchedulerLeaseMapper leaseMapper,
                                           @Value("${canvas.bi.delivery.scheduler.lease-owner-id:}") String configuredOwnerId) {
        this.leaseMapper = leaseMapper;
        this.ownerId = normalizeOwner(configuredOwnerId);
    }

    public boolean acquire(Long tenantId, String leaseKey, Duration ttl) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedLeaseKey = required(leaseKey, "leaseKey");
        Duration scopedTtl = validateTtl(ttl);
        LocalDateTime now = LocalDateTime.now();
        BiDeliverySchedulerLeaseDO row = new BiDeliverySchedulerLeaseDO();
        row.setTenantId(scopedTenantId);
        row.setLeaseKey(scopedLeaseKey);
        row.setOwnerId(ownerId);
        row.setLastAcquiredAt(now);
        row.setLeaseUntil(now.plus(scopedTtl));
        leaseMapper.tryAcquire(row, now);
        return ownsLease(scopedTenantId, scopedLeaseKey, now);
    }

    public void release(Long tenantId, String leaseKey) {
        leaseMapper.release(
                normalizeTenant(tenantId),
                required(leaseKey, "leaseKey"),
                ownerId,
                LocalDateTime.now());
    }

    String ownerId() {
        return ownerId;
    }

    private boolean ownsLease(Long tenantId, String leaseKey, LocalDateTime acquiredAt) {
        BiDeliverySchedulerLeaseDO current = leaseMapper.findByKey(tenantId, leaseKey);
        return current != null
                && ownerId.equals(current.getOwnerId())
                && current.getLeaseUntil() != null
                && current.getLeaseUntil().isAfter(acquiredAt);
    }

    private Duration validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return ttl;
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOwner(String configuredOwnerId) {
        if (configuredOwnerId != null && !configuredOwnerId.isBlank()) {
            return configuredOwnerId.trim();
        }
        return "bi-delivery-" + UUID.randomUUID();
    }
}
