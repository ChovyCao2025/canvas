package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单次画布执行的上下文。
 * 注意：使用 @Getter/@Setter 而非 @Data，避免对含可变 Map/AtomicBoolean 的对象
 * 生成基于所有字段的 equals()/hashCode()，那会导致并发场景下行为不可预测。
 */
@Getter
@Setter
@ToString(exclude = {"scheduledHubTimeouts"})
public class ExecutionContext {

    /** 执行实例 ID（UUID）。 */
    private String executionId;

    /** 画布 ID。 */
    private Long canvasId;

    /** 执行时锁定的版本 ID。 */
    private Long versionId;

    /** 触发用户 ID。 */
    private String userId;

    /** 压测批次 ID，普通业务流量为空。 */
    private String perfRunId;

    /** 触发类型（DIRECT_CALL/MQ/BEHAVIOR/DRY_RUN...）。 */
    private String triggerType;

    /** 触发器节点类型（MQ_TRIGGER / EVENT_TRIGGER 等），DLQ 重放时需要 */
    private String triggerNodeType;

    /** 路由匹配 key（MQ=topicKey，BEHAVIOR=eventCode），DLQ 重放时需要 */
    private String matchKey;

    /** 触发器携带的原始数据（写入 ctx 时的 payload） */
    private Map<String, Object> triggerPayload = new HashMap<>();

    /** 各节点产出数据历史（nodeId → {fieldKey → value}），供轨迹查询；保留插入顺序用于超限淘汰 */
    private final Map<String, Map<String, Object>> nodeOutputs =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** 扁平化快速查找 Map，O(1)。由 putNodeOutput 维护；由 nodeOutputs 派生，不参与持久化 */
    @JsonIgnore
    private final Map<String, Object> flatContext = new ConcurrentHashMap<>();

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

    /** LOOP 节点迭代次数（nodeId → count），持久化以支持挂起恢复后的边界控制。 */
    private Map<String, Integer> loopIterations = new ConcurrentHashMap<>();

    /** GOTO 节点跳转次数（nodeId → count），持久化以防无界跳转。 */
    private Map<String, Integer> jumpCounts = new ConcurrentHashMap<>();

    /**
     * 每节点的执行门控，分离互斥锁与 repeat 信号。
     * 不序列化：恢复时通过 {@link #getGate(String)} 懒建。
     */
    @JsonIgnore
    private final Map<String, NodeGate> nodeGates = new ConcurrentHashMap<>();

    /**
     * resume-lock 的持有令牌（acquireResumeLock 时生成的 UUID）。
     * 用于 releaseResumeLock 的原子 check-then-del（防止错误释放其他实例持有的锁）。
     * 不序列化：每次 resume 重新获取新锁，token 不需要跨 WAIT 持久化。
     */
    @JsonIgnore
    private String resumeLockToken;

    /** 当前执行是否跳过配额扣减；仅运行期使用，不随 WAIT 上下文持久化。 */
    @JsonIgnore
    private boolean quotaBypass;

    /**
     * 已为 Hub 节点调度过超时任务的节点 ID 集合（防重复调度）。
     * 不序列化：多阶段恢复后首次触发时重新调度。
     */
    @JsonIgnore
    private final Set<String> scheduledHubTimeouts = ConcurrentHashMap.newKeySet();

    /** 累计估算大小（字节），@JsonIgnore 不参与序列化 */
    @JsonIgnore
    private final AtomicInteger approxSizeBytes = new AtomicInteger(0);

    /** 上下文估算大小上限，超过后由调用方判断是否中止。 */
    private static final int MAX_SIZE_BYTES  = 1024 * 1024; // 1MB（设计文档 13.7节）
    /** 上下文估算大小预警阈值。 */
    private static final int WARN_SIZE_BYTES = 512 * 1024; // 512KB 预警

    // ── 写入节点输出 ────────────────────────────────────────────

    /** 获取所有节点的输出（只读），用于聚合评估等需要跨节点读输出的场景 */
    public Map<String, Map<String, Object>> getNodeOutputs() {
        synchronized (nodeOutputs) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(nodeOutputs));
        }
    }

    @JsonSetter("nodeOutputs")
    void restoreNodeOutputs(Map<String, Map<String, Object>> restoredNodeOutputs) {
        synchronized (nodeOutputs) {
            nodeOutputs.clear();
            if (restoredNodeOutputs != null) {
                restoredNodeOutputs.forEach((nodeId, output) ->
                        nodeOutputs.put(nodeId, output == null ? Map.of() : new LinkedHashMap<>(output)));
            }
        }
        rebuildDerivedState();
    }

    /** 获取派生的扁平上下文快照（只读），内部写入必须走 putNodeOutput/putRuntimeContextValue。 */
    @JsonIgnore
    public Map<String, Object> getFlatContext() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(flatContext));
    }

    /**
     * 写入或记录 put Node Output 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param output output 方法执行所需的业务参数
     */
    public synchronized void putNodeOutput(String nodeId, Map<String, Object> output) {
        Map<String, Object> snapshot = output == null ? Map.of() : new LinkedHashMap<>(output);
        int incomingSize = estimateNodeOutputSize(nodeId, snapshot);
        if (incomingSize > MAX_SIZE_BYTES) {
            throw new ContextOverflowException(
                    "Context output for nodeId=" + nodeId + " exceeds 1MB limit: "
                            + incomingSize + " bytes",
                    approxSizeBytes.get());
        }

        removeNodeOutput(nodeId);
        evictOldestNodesUntilFits(incomingSize);
        if (approxSizeBytes.get() + incomingSize > MAX_SIZE_BYTES) {
            throw new ContextOverflowException(
                    "Context exceeds 1MB limit: " + approxSizeBytes.get()
                            + " bytes. Node output rejected for nodeId=" + nodeId,
                    approxSizeBytes.get());
        }

        nodeOutputs.put(nodeId, snapshot);
        snapshot.forEach((fieldKey, value) ->
                flatContext.put(namespacedKey(nodeId, fieldKey), value));
        approxSizeBytes.set(estimateSerializedStateSize());

        if (approxSizeBytes.get() > WARN_SIZE_BYTES) {
            // 超过 512KB 提前预警，便于排查超大字段；日志在调用侧统一补充执行信息。
        }
    }

    /** 是否超过 1MB 上限 */
    @JsonIgnore
    public boolean isOversized() { return approxSizeBytes.get() > MAX_SIZE_BYTES; }

    /** 获取估算大小（字节） */
    @JsonIgnore
    public int getApproxSizeBytes() { return approxSizeBytes.get(); }

    /**
     * 查询或读取 get Context Value 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param fieldKey fieldKey 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
// ── 读取上下文字段，O(1) ─────────────────────────────────────

    public Object getContextValue(String fieldKey) {
        Object val = flatContext.get(fieldKey);
        if (val != null) {
            return val;
        }

        Object latest = null;
        synchronized (nodeOutputs) {
            for (Map<String, Object> output : nodeOutputs.values()) {
                Object candidate = output.get(fieldKey);
                if (candidate != null) {
                    latest = candidate;
                }
            }
        }
        if (latest != null) {
            return latest;
        }

        // 节点输出优先于触发载荷，允许下游节点读取上游加工后的同名字段。
        return triggerPayload.get(fieldKey);
    }

    /** Read one output field from a specific node without flat-key collision risk. */
    public Object getNodeOutput(String nodeId, String fieldKey) {
        synchronized (nodeOutputs) {
            Map<String, Object> output = nodeOutputs.get(nodeId);
            return output == null ? null : output.get(fieldKey);
        }
    }

    /** 写入运行期恢复信号等非节点输出值，避免调用方直接修改派生 flatContext。 */
    public synchronized void putRuntimeContextValue(String fieldKey, Object value) {
        boolean hadPrevious = triggerPayload.containsKey(fieldKey);
        Object previous = triggerPayload.get(fieldKey);
        int previousSize = hadPrevious ? estimateMapEntrySize(fieldKey, previous) : 0;
        int incomingSize = estimateMapEntrySize(fieldKey, value);

        approxSizeBytes.addAndGet(-previousSize);
        evictOldestNodesUntilFits(incomingSize);
        if (approxSizeBytes.get() + incomingSize > MAX_SIZE_BYTES) {
            if (hadPrevious) {
                approxSizeBytes.addAndGet(previousSize);
            }
            throw new ContextOverflowException(
                    "Context exceeds 1MB limit: " + approxSizeBytes.get()
                            + " bytes. Runtime value rejected for key=" + fieldKey,
                    approxSizeBytes.get());
        }

        triggerPayload.put(fieldKey, value);
        approxSizeBytes.set(estimateSerializedStateSize());
    }

    /** 导出跨旅程/子流程可携带的上下文，保留裸字段兼容性并包含 namespaced 字段。 */
    public Map<String, Object> exportContextValues() {
        Map<String, Object> exported = new LinkedHashMap<>(triggerPayload);
        synchronized (nodeOutputs) {
            nodeOutputs.values().forEach(exported::putAll);
        }
        exported.putAll(flatContext);
        return exported;
    }

    /** 反序列化或批量恢复后重建派生索引和大小估算。 */
    public synchronized void rebuildDerivedState() {
        flatContext.clear();
        synchronized (nodeOutputs) {
            nodeOutputs.forEach((nodeId, output) ->
                    output.forEach((fieldKey, value) ->
                            flatContext.put(namespacedKey(nodeId, fieldKey), value)));
        }
        approxSizeBytes.set(estimateSerializedStateSize());
    }

    private void evictOldestNodesUntilFits(int incomingSize) {
        synchronized (nodeOutputs) {
            Iterator<Map.Entry<String, Map<String, Object>>> it = nodeOutputs.entrySet().iterator();
            while (approxSizeBytes.get() + incomingSize > MAX_SIZE_BYTES && it.hasNext()) {
                Map.Entry<String, Map<String, Object>> entry = it.next();
                removeFlatOutput(entry.getKey(), entry.getValue());
                approxSizeBytes.addAndGet(-estimateNodeOutputSize(entry.getKey(), entry.getValue()));
                it.remove();
            }
        }
    }

    private void removeNodeOutput(String nodeId) {
        Map<String, Object> previous;
        synchronized (nodeOutputs) {
            previous = nodeOutputs.remove(nodeId);
        }
        if (previous != null) {
            removeFlatOutput(nodeId, previous);
            approxSizeBytes.addAndGet(-estimateNodeOutputSize(nodeId, previous));
        }
    }

    private void removeFlatOutput(String nodeId, Map<String, Object> output) {
        output.keySet().forEach(fieldKey -> flatContext.remove(namespacedKey(nodeId, fieldKey)));
    }

    private static String namespacedKey(String nodeId, String fieldKey) {
        return nodeId + "." + fieldKey;
    }

    private static int estimateNodeOutputSize(String nodeId, Map<String, Object> output) {
        int size = 2; // object braces
        for (Map.Entry<String, Object> entry : output.entrySet()) {
            size += estimateMapEntrySize(entry.getKey(), entry.getValue());
        }
        size += nodeId.length() + 4;
        return size;
    }

    private int estimateSerializedStateSize() {
        int size = 512; // fixed ExecutionContext field overhead approximation
        synchronized (nodeOutputs) {
            for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
                size += estimateNodeOutputSize(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Object> entry : triggerPayload.entrySet()) {
            size += estimateMapEntrySize(entry.getKey(), entry.getValue());
        }
        return size;
    }

    private static int estimateMapEntrySize(String key, Object value) {
        return String.valueOf(key).length() + estimateValueSize(value) + 4;
    }

    private static int estimateValueSize(Object value) {
        if (value == null) {
            return 4;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.length() + 2;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value).length();
        }
        if (value instanceof Map<?, ?> map) {
            int size = 2;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                size += String.valueOf(entry.getKey()).length() + 4;
                size += estimateValueSize(entry.getValue());
            }
            return size;
        }
        if (value instanceof Iterable<?> iterable) {
            int size = 2;
            for (Object item : iterable) {
                size += estimateValueSize(item) + 1;
            }
            return size;
        }
        if (value.getClass().isArray()) {
            int size = 2;
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                size += estimateValueSize(java.lang.reflect.Array.get(value, i)) + 1;
            }
            return size;
        }
        return String.valueOf(value).length() + 2;
    }

    /**
     * 执行 set Node Status 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param status status 状态值或状态筛选条件
     */
// ── 节点状态 ────────────────────────────────────────────────

    public void setNodeStatus(String nodeId, NodeStatus status) {
        nodeStatuses.put(nodeId, status);
    }

    /**
     * 原子 putIfAbsent：仅在 key 不存在时写入，返回是否实际写入。
     * 供 writeSkippedNodes 使用，防止覆盖并发执行路径已写入的状态。
     */
    public boolean setNodeStatusIfAbsent(String nodeId, NodeStatus status) {
        return nodeStatuses.putIfAbsent(nodeId, status) == null;
    }

    /**
     * 原子 compute：仅在节点未到达终态时写入，返回是否实际写入。
     * 终态定义与 isNodeDone 一致。供 markSkipped 使用，防止覆盖已完成节点的状态。
     */
    public boolean setNodeStatusIfNotDone(String nodeId, NodeStatus status) {
        boolean[] updated = {false};
        nodeStatuses.compute(nodeId, (k, current) -> {
            // 已进入终态的节点不再被 SKIPPED 等状态覆盖，避免并发分支破坏真实执行结果。
            if (!isTerminalStatus(current)) {
                updated[0] = true;
                return status;
            }
            return current;
        });
        return updated[0];
    }

    /**
     * 判断 is Terminal Status 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param s s 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean isTerminalStatus(NodeStatus s) {
        return s == NodeStatus.SUCCESS || s == NodeStatus.FAILED
                || s == NodeStatus.TIMEOUT || s == NodeStatus.SUPPRESSED
                || s == NodeStatus.SKIPPED || s == NodeStatus.PARTIAL_FAIL;
    }

    /**
     * 查询或读取 get Node Status 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
    public NodeStatus getNodeStatus(String nodeId) {
        return nodeStatuses.getOrDefault(nodeId, NodeStatus.PENDING);
    }

    /**
     * 执行 reset Node Status For Reentry 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     */
    public void resetNodeStatusForReentry(String nodeId) {
        nodeStatuses.computeIfPresent(nodeId, (key, status) ->
                // 重入只清理可重新执行的非终态/成功态，失败类终态保留给恢复和告警判断。
                status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT || status == NodeStatus.PARTIAL_FAIL
                        ? status
                        : null);
    }

    /**
     * 判断 is Node Done 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean isNodeDone(String nodeId) {
        NodeStatus s = getNodeStatus(nodeId);
        return s == NodeStatus.SUCCESS || s == NodeStatus.FAILED
                || s == NodeStatus.TIMEOUT || s == NodeStatus.SUPPRESSED
                || s == NodeStatus.SKIPPED || s == NodeStatus.PARTIAL_FAIL;
    }

    /**
     * 查询或读取 get Gate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
// ── 每节点并发锁（懒建，不序列化） ─────────────────────────────

    /** 获取节点执行门控，不存在时懒建（恢复执行后首次访问时自动创建）。 */
    public NodeGate getGate(String nodeId) {
        return nodeGates.computeIfAbsent(nodeId, k -> new NodeGate());
    }
}
