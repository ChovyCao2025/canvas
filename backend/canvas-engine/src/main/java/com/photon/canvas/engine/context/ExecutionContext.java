package com.photon.canvas.engine.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次画布执行的上下文。
 *
 * 设计文档第五章：
 * - flatContext 提供 O(1) 字段查找（Last Writer Wins）
 * - nodeOutputs 保留完整历史用于轨迹查询
 * - nodeLocks 为每个节点提供 CAS 并发保护（repeat 机制）
 *
 * 序列化说明：
 * - @JsonIgnore 字段不持久化到 Redis，反序列化后按需重建
 * - hubStartTimes 持久化，用于多阶段恢复后继续检查 Hub 超时
 */
@Data
public class ExecutionContext {

    private String executionId;
    private Long   canvasId;
    private Long   versionId;
    private String userId;
    private String triggerType;

    /** 触发器携带的原始数据（写入 ctx 时的 payload） */
    private Map<String, Object> triggerPayload = new HashMap<>();

    /** 各节点产出数据历史（nodeId → {fieldKey → value}），供轨迹查询 */
    private Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();

    /** 扁平化快速查找 Map，O(1)。由 putNodeOutput 维护 */
    private Map<String, Object> flatContext = new HashMap<>();

    /** 各节点执行状态（需持久化，多阶段恢复时 LOGIC_RELATION 依赖此状态） */
    private Map<String, NodeStatus> nodeStatuses = new ConcurrentHashMap<>();

    /** 防资损：已发放权益 */
    private volatile boolean benefitGranted = false;

    /** 防资损：已触达用户 */
    private volatile boolean userReached = false;

    /** 子流程调用链，防循环（callStack 中记录正在执行的 canvasId） */
    private List<Long> callStack = new ArrayList<>();

    /**
     * Hub 节点首次进入等待的时间戳（ms）。
     * 持久化：多阶段恢复后仍需检查 Hub 是否已超时。
     */
    private Map<String, Long> hubStartTimes = new ConcurrentHashMap<>();

    /**
     * 每节点的 waitProcess 并发保护锁。
     * 不序列化：AtomicBoolean 无法 JSON 化；恢复时通过 getLock() 懒建。
     */
    @JsonIgnore
    private final Map<String, AtomicBoolean> nodeLocks = new ConcurrentHashMap<>();

    /**
     * 已为 Hub 节点调度过超时任务的节点 ID 集合（防重复调度）。
     * 不序列化：多阶段恢复后首次触发时重新调度。
     */
    @JsonIgnore
    private final Set<String> scheduledHubTimeouts = ConcurrentHashMap.newKeySet();

    // ── 写入节点输出 ────────────────────────────────────────────

    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, output);
        flatContext.putAll(output);   // O(k)，Last Writer Wins
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
        return s == NodeStatus.SUCCESS || s == NodeStatus.FAILED
                || s == NodeStatus.SKIPPED || s == NodeStatus.PARTIAL_FAIL;
    }

    // ── 每节点并发锁（懒建，不序列化） ─────────────────────────────

    /**
     * 获取节点的 waitProcess 锁。初始值 true（可被 CAS 抢占）。
     * 反序列化后 nodeLocks 为空 Map，首次访问时自动创建。
     */
    public AtomicBoolean getLock(String nodeId) {
        return nodeLocks.computeIfAbsent(nodeId, k -> new AtomicBoolean(true));
    }
}
