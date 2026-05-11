package com.photon.canvas.engine.context;

import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次画布执行的上下文，全程随 Reactor 管道传递（通过 Reactor Context API）。
 * flatContext 提供 O(1) 字段查找；nodeOutputs 保留历史供轨迹查询。
 */
@Data
public class ExecutionContext {

    private String executionId;
    private Long   canvasId;
    private Long   versionId;
    private String userId;
    private String triggerType;

    /** 触发器携带的原始数据 */
    private Map<String, Object> triggerPayload = new HashMap<>();

    /** 各节点产出数据历史（nodeId → {fieldKey → value}） */
    private Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();

    /** 扁平化快速查找 Map，O(1)。Last Writer Wins */
    private Map<String, Object> flatContext = new HashMap<>();

    /** 各节点执行状态 */
    private Map<String, NodeStatus> nodeStatuses = new ConcurrentHashMap<>();

    /** 每个节点的并发保护锁（waitProcess）：true=可被抢占，false=正在执行 */
    private Map<String, AtomicBoolean> nodeLocks = new ConcurrentHashMap<>();

    /** 防资损：已发放权益 */
    private volatile boolean benefitGranted = false;

    /** 防资损：已触达用户 */
    private volatile boolean userReached = false;

    /** 子流程调用链，防循环 */
    private List<Long> callStack = new ArrayList<>();

    // ── 写入节点输出 ────────────────────────────────────────────

    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, output);
        flatContext.putAll(output);   // O(k)，k = output字段数
    }

    // ── 读取上下文字段，O(1) ─────────────────────────────────────

    public Object getContextValue(String fieldKey) {
        Object val = flatContext.get(fieldKey);
        return val != null ? val : triggerPayload.get(fieldKey);
    }

    // ── 节点状态 ────────────────────────────────────────────────

    public void setNodeStatus(String nodeId, NodeStatus status) {
        nodeStatuses.put(nodeId, status);
    }

    public NodeStatus getNodeStatus(String nodeId) {
        return nodeStatuses.getOrDefault(nodeId, NodeStatus.PENDING);
    }

    public boolean isNodeDone(String nodeId) {
        NodeStatus s = getNodeStatus(nodeId);
        return s == NodeStatus.SUCCESS || s == NodeStatus.FAILED || s == NodeStatus.SKIPPED;
    }

    // ── 每节点并发锁 ─────────────────────────────────────────────

    public AtomicBoolean getLock(String nodeId) {
        return nodeLocks.computeIfAbsent(nodeId, k -> new AtomicBoolean(true));
    }
}
