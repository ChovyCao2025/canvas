package org.chovy.canvas.cdp.domain;

import java.util.List;

public interface AudienceSnapshotRepository {

    List<String> resolveUsers(Long audienceId);

    AudienceSnapshot save(AudienceSnapshot snapshot);

    AudienceSnapshot findSnapshot(Long snapshotId);

    String defaultSnapshotMode(Long audienceId);
}
