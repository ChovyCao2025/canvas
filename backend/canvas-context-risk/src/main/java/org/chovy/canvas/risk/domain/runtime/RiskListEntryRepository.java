package org.chovy.canvas.risk.domain.runtime;

import java.util.Optional;

/**
 * 风控名单条目仓储接口。
 */
@FunctionalInterface
public interface RiskListEntryRepository {

    /**
     * 按主体哈希查找当前可匹配的名单条目。
     */
    Optional<RiskListEntry> findActiveEntry(Long tenantId, String listKey, String subjectHash);
}
