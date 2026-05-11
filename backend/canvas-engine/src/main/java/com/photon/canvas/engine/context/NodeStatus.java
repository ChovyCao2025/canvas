package com.photon.canvas.engine.context;

public enum NodeStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED,
    PARTIAL_FAIL   // PRIORITY 节点所有分支失败但有 nextNodeId
}
