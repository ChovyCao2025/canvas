package org.chovy.canvas.cdp.domain;

import java.util.List;

/**
 * 定义 AudienceSnapshot 的持久化访问契约。
 */
public interface AudienceSnapshotRepository {

    /**
     * 执行 resolveUsers 对应的 CDP 业务操作。
     */
    List<String> resolveUsers(Long audienceId);

    /**
     * 保存save。
     */
    AudienceSnapshot save(AudienceSnapshot snapshot);

    /**
     * 查找Snapshot。
     */
    AudienceSnapshot findSnapshot(Long snapshotId);

    /**
     * 返回默认的Snapshot Mode。
     */
    String defaultSnapshotMode(Long audienceId);
}
