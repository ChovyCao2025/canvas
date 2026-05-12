package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次画布执行的上下文。
 *
 * 注意：使用 @Getter/@Setter 而非 @Data，避免对含可变 Map/AtomicBoolean 的对象
 * 生成基于所有字段的 equals()/hashCode()，那会导致并发场景下行为不可预测。
 */
@Getter
@Setter
@ToString(exclude = {"nodeLocks", "scheduledHubTimeouts"})
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

    /** 累计估算大小（字节），@JsonIgnore 不参与序列化 */
    @JsonIgnore
    private int approxSizeBytes = 0;

    private static final int MAX_SIZE_BYTES  = 1024 * 1024; // 1MB（设计文档 13.7节）
    private static final int WARN_SIZE_BYTES = 512 * 1024;  // 512KB 预警

    // ── 写入节点输出 ────────────────────────────────────────────

    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, output);
        flatContext.putAll(output);

        // 大小监控：累加估算字节数（设计文档 13.7节）
        // 使用轻量累加而非每次 JSON 序列化，避免 O(n) 开销
        output.forEach((k, v) ->
            approxSizeBytes += k.length() + (v != null ? v.toString().length() : 4));

        if (approxSizeBytes > MAX_SIZE_BYTES) {
            // 不截断（截断可能破坏防资损逻辑），仅记录 WARN
            // 调用方可通过检查 isOversized() 决定是否中止
        } else if (approxSizeBytes > WARN_SIZE_BYTES) {
            // 超过 512KB 提前预警，便于排查超大字段
        }
    }

    /** 是否超过 1MB 上限 */
    public boolean isOversized() { return approxSizeBytes > MAX_SIZE_BYTES; }

    /** 获取估算大小（字节） */
    public int getApproxSizeBytes() { return approxSizeBytes; }

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
