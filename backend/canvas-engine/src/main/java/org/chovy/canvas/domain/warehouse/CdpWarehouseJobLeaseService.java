package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseJobLeaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CdpWarehouseJobLeaseService {

    private final CdpWarehouseJobLeaseMapper leaseMapper;
    private final String ownerId;

    public CdpWarehouseJobLeaseService(CdpWarehouseJobLeaseMapper leaseMapper,
                                       @Value("${canvas.warehouse.lease.owner-id:}") String configuredOwnerId) {
        this.leaseMapper = leaseMapper;
        this.ownerId = normalizeOwner(configuredOwnerId);
    }

    public boolean runWithLease(Long tenantId, String leaseKey, Duration ttl, Supplier<Boolean> work) {
        if (work == null) {
            throw new IllegalArgumentException("work is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedLeaseKey = required(leaseKey, "leaseKey");
        Duration scopedTtl = validateTtl(ttl);
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseJobLeaseDO row = new CdpWarehouseJobLeaseDO();
        row.setTenantId(scopedTenantId);
        row.setLeaseKey(scopedLeaseKey);
        row.setOwnerId(ownerId);
        row.setLastAcquiredAt(now);
        row.setLeaseUntil(now.plus(scopedTtl));
        leaseMapper.tryAcquire(row, now);
        if (!ownsLease(scopedTenantId, scopedLeaseKey, now)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(work.get());
        } finally {
            leaseMapper.release(scopedTenantId, scopedLeaseKey, ownerId, LocalDateTime.now());
        }
    }

    private boolean ownsLease(Long tenantId, String leaseKey, LocalDateTime acquiredAt) {
        CdpWarehouseJobLeaseDO current = leaseMapper.findByKey(tenantId, leaseKey);
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
        return "warehouse-" + UUID.randomUUID();
    }
}
