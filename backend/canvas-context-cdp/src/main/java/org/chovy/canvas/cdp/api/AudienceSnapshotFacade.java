package org.chovy.canvas.cdp.api;

import java.util.List;

public interface AudienceSnapshotFacade {

    AudienceSnapshotView lockSnapshot(AudienceSnapshotLockCommand command);

    String defaultModeForAudience(Long audienceId);

    List<String> users(Long snapshotId);

    boolean contains(Long snapshotId, String userId);
}
