package org.chovy.canvas.domain.bi.subscription;

public interface BiSnapshotRenderer {

    boolean configured();

    BiSnapshotRenderResult render(BiSnapshotRenderRequest request);
}
