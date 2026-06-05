package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpUserIndexDO;
import org.chovy.canvas.dal.mapper.CdpUserIndexMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StableUserIndexService {

    private static final long DEFAULT_TENANT_ID = 0L;
    private static final int MAX_ALLOCATE_ATTEMPTS = 3;

    private final CdpUserIndexMapper mapper;

    public long getOrCreateIndex(Long tenantId, String userId) {
        Long normalizedTenantId = tenantId == null ? DEFAULT_TENANT_ID : tenantId;
        String normalizedUserId = normalizeUserId(userId);

        CdpUserIndexDO existing = mapper.selectByTenantAndUser(normalizedTenantId, normalizedUserId);
        if (existing != null && existing.getUserIndex() != null) {
            return existing.getUserIndex();
        }

        for (int attempt = 0; attempt < MAX_ALLOCATE_ATTEMPTS; attempt++) {
            Long nextIndex = mapper.nextIndexForTenant(normalizedTenantId);
            if (nextIndex == null || nextIndex < 1) {
                nextIndex = 1L;
            }

            CdpUserIndexDO row = new CdpUserIndexDO();
            row.setTenantId(normalizedTenantId);
            row.setUserId(normalizedUserId);
            row.setUserIndex(nextIndex);

            try {
                mapper.insert(row);
                return nextIndex;
            } catch (DuplicateKeyException ex) {
                CdpUserIndexDO reloaded = mapper.selectByTenantAndUser(normalizedTenantId, normalizedUserId);
                if (reloaded != null && reloaded.getUserIndex() != null) {
                    return reloaded.getUserIndex();
                }
            }
        }

        throw new IllegalStateException("failed to allocate stable user index");
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return userId.trim();
    }
}
