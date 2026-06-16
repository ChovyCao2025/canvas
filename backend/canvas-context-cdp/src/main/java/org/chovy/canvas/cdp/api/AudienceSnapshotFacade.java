package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 定义 AudienceSnapshotFacade 对外暴露的 CDP 业务能力。
 */
public interface AudienceSnapshotFacade {

    /**
     * 执行 lockSnapshot 对应的 CDP 业务操作。
     */
    AudienceSnapshotView lockSnapshot(AudienceSnapshotLockCommand command);

    /**
     * 返回默认的Mode For Audience。
     */
    String defaultModeForAudience(Long audienceId);

    /**
     * 执行 users 对应的 CDP 业务操作。
     */
    List<String> users(Long snapshotId);

    /**
     * 执行 contains 对应的 CDP 业务操作。
     */
    boolean contains(Long snapshotId, String userId);
}
