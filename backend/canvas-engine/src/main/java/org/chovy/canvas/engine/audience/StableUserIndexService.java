package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpUserIndexDO;
import org.chovy.canvas.dal.mapper.CdpUserIndexMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * StableUserIndexService 参与 engine.audience 场景的画布执行引擎处理。
 */
@Service
@RequiredArgsConstructor
public class StableUserIndexService {

    private static final long DEFAULT_TENANT_ID = 0L;
    private static final int MAX_ALLOCATE_ATTEMPTS = 3;

    private final CdpUserIndexMapper mapper;

    /**
     * 获取或分配租户内稳定用户序号。
     *
     * <p>方法先按租户和用户 ID 查询已有序号；未命中时申请下一个序号并插入索引表。并发冲突时会回读已插入记录，
     * 最多重试三次，保证同一用户长期映射到同一个 bitmap index。
     *
     * @param tenantId 租户 ID，空值按默认租户处理
     * @param userId 用户 ID，不能为空
     * @return 租户内稳定递增的用户序号
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DuplicateKeyException ex) {
                CdpUserIndexDO reloaded = mapper.selectByTenantAndUser(normalizedTenantId, normalizedUserId);
                if (reloaded != null && reloaded.getUserIndex() != null) {
                    return reloaded.getUserIndex();
                }
            }
        }

        throw new IllegalStateException("failed to allocate stable user index");
    }

    /**
     * 校验并规范化用户 ID。
     *
     * @param userId 原始用户 ID
     * @return 去除首尾空白后的用户 ID
     */
    private String normalizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return userId.trim();
    }
}
