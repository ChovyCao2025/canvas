package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单次画布执行的上下文。
 * 注意：使用 @Getter/@Setter 而非 @Data，避免对含可变 Map/AtomicBoolean 的对象
 * 生成基于所有字段的 equals()/hashCode()，那会导致并发场景下行为不可预测。
 */
@Getter
@Setter
@ToString(exclude = {"scheduledHubTimeouts"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionContext {

    /** 执行实例 ID（UUID）。 */
    private String executionId;

    /** 画布 ID。 */
    private Long canvasId;

    /** 所属租户 ID。 */
    private Long tenantId;

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
    private Map<String, Object> triggerPayload = new ConcurrentHashMap<>();

    /** 各节点产出数据历史（nodeId → {fieldKey → value}），供轨迹查询 */
    private final Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();

    /** 扁平化快速查找 Map，O(1)。由 putNodeOutput 维护 */
    @JsonIgnore
    private final Map<String, Object> flatContext = new ConcurrentHashMap<>();

    /** 节点输出写入锁，保证分组输出、扁平索引和大小估算保持同一快照。 */
    @JsonIgnore
    private final Object outputLock = new Object();

    /** 扁平索引 key 的当前所有者节点，处理并发分支同名字段覆盖。 */
    @JsonIgnore
    private final Map<String, String> flatKeyOwners = new ConcurrentHashMap<>();

    /** 每个节点写入过的扁平索引 key，用于覆盖节点输出时清理旧字段。 */
    @JsonIgnore
    private final Map<String, Set<String>> nodeFlatKeys = new ConcurrentHashMap<>();

    /** 兼容裸字段读取的最新节点输出值；不序列化，恢复时由 nodeOutputs 重建。 */
    @JsonIgnore
    private final Map<String, Object> latestOutputValues = new ConcurrentHashMap<>();

    /** latestOutputValues 的所属节点，用于覆盖/淘汰节点输出时清理裸字段兼容索引。 */
    @JsonIgnore
    private final Map<String, String> latestOutputOwners = new ConcurrentHashMap<>();

    /** 各节点执行状态（需持久化，多阶段恢复和汇聚判断依赖此状态） */
    private Map<String, NodeStatus> nodeStatuses = new ConcurrentHashMap<>();

    /** 防资损：已发放权益 */
    private volatile boolean benefitGranted = false;

    /** 防资损：已触达用户 */
    private volatile boolean userReached = false;

    /** 子流程调用链，防循环（callStack 中记录正在执行的 canvasId） */
    private List<Long> callStack = new CopyOnWriteArrayList<>();

    /**
     * Hub 节点首次进入等待的时间戳（ms）。
     * 持久化：多阶段恢复后仍需检查 Hub 是否已超时。
     */
    private Map<String, Long> hubStartTimes = new ConcurrentHashMap<>();

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

    private static final ObjectMapper SIZE_OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_MAX_SIZE_BYTES  = 1024 * 1024;
    private static final int DEFAULT_WARN_SIZE_BYTES = 512 * 1024;

    /** 上下文硬上限，超过后拒绝写入。 */
    @JsonIgnore
    private int maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;

    /** 上下文预警阈值。 */
    @JsonIgnore
    private int warnSizeBytes = DEFAULT_WARN_SIZE_BYTES;

    // ── 写入节点输出 ────────────────────────────────────────────

    /** 获取所有节点的输出（只读），用于聚合评估等需要跨节点读输出的场景 */
    public Map<String, Map<String, Object>> getNodeOutputs() {
        synchronized (outputLock) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(nodeOutputs));
        }
    }

    public void setNodeOutputs(Map<String, Map<String, Object>> nodeOutputs) {
        synchronized (outputLock) {
            this.nodeOutputs.clear();
            if (nodeOutputs != null) {
                nodeOutputs.forEach((nodeId, output) -> {
                    if (nodeId != null) {
                        this.nodeOutputs.put(nodeId, immutableOutputSnapshot(output));
                    }
                });
            }
            rebuildDerivedStateLocked();
        }
    }

    public Object getNodeOutput(String nodeId, String fieldKey) {
        synchronized (outputLock) {
            Map<String, Object> output = nodeOutputs.get(nodeId);
            return output == null ? null : output.get(fieldKey);
        }
    }

    public Map<String, Object> getTriggerPayload() {
        return readOnlyMapSnapshot(triggerPayload);
    }

    public Map<String, Object> getFlatContext() {
        return readOnlyMapSnapshot(flatContext);
    }

    public void setTriggerPayload(Map<String, Object> triggerPayload) {
        Map<String, Object> snapshot = mutableDeepCopyMap(triggerPayload);
        synchronized (outputLock) {
            int candidateSize = serializedContextSize(snapshot, nodeOutputs);
            if (candidateSize > maxSizeBytes) {
                throw contextOverflow(candidateSize);
            }
            this.triggerPayload = new ConcurrentHashMap<>(snapshot);
            approxSizeBytes.set(candidateSize);
        }
    }

    public void putTriggerPayloadValues(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        synchronized (outputLock) {
            Map<String, Object> candidate = mutableDeepCopyMap(triggerPayload);
            mutableDeepCopyMap(values).forEach((key, value) -> {
                if (value != null) {
                    candidate.put(key, value);
                }
            });
            int candidateSize = serializedContextSize(candidate, nodeOutputs);
            if (candidateSize > maxSizeBytes) {
                throw contextOverflow(candidateSize);
            }
            triggerPayload = new ConcurrentHashMap<>(candidate);
            approxSizeBytes.set(candidateSize);
        }
    }

    public Map<String, Object> exportContextValues() {
        Map<String, Object> values = new LinkedHashMap<>(triggerPayload);
        values.putAll(latestOutputValues);
        values.putAll(flatContext);
        return values;
    }

    public void setCallStack(List<Long> callStack) {
        this.callStack = callStack == null
                ? new CopyOnWriteArrayList<>()
                : new CopyOnWriteArrayList<>(callStack);
    }

    /**
     * 写入或记录 put Node Output 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param output output 方法执行所需的业务参数
     */
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Map<String, Object> snapshot = immutableOutputSnapshot(output);

        synchronized (outputLock) {
            LinkedHashMap<String, Map<String, Object>> candidate = new LinkedHashMap<>(nodeOutputs);
            candidate.remove(nodeId);
            candidate.put(nodeId, snapshot);
            fitUnderLimit(candidate);
            nodeOutputs.clear();
            nodeOutputs.putAll(candidate);
            rebuildDerivedStateLocked();
        }

        if (approxSizeBytes.get() > warnSizeBytes) {
            // 超过 512KB 提前预警，便于排查超大字段
        }
    }

    private Map<String, Object> immutableOutputSnapshot(Map<String, Object> output) {
        return readOnlyMapSnapshot(output);
    }

    private void clearOwnedFlatKeys(String nodeId) {
        Set<String> previousKeys = nodeFlatKeys.remove(nodeId);
        if (previousKeys == null || previousKeys.isEmpty()) {
            return;
        }
        for (String flatKey : previousKeys) {
            if (nodeId.equals(flatKeyOwners.get(flatKey))) {
                flatContext.remove(flatKey);
                flatKeyOwners.remove(flatKey);
            }
        }
    }

    private void putOwnedFlatKey(String nodeId, String key, Object value, Set<String> writtenFlatKeys) {
        flatContext.put(key, value);
        flatKeyOwners.put(key, nodeId);
        writtenFlatKeys.add(key);
    }

    private String qualifiedOutputKey(String nodeId, String fieldKey) {
        return nodeId + "." + fieldKey;
    }

    private int currentNodeOutputSizeLocked() {
        return serializedContextSize(triggerPayload, nodeOutputs);
    }

    private int serializedNodeOutputSize(Map<String, Map<String, Object>> outputs) {
        try {
            return SIZE_OBJECT_MAPPER.writeValueAsBytes(outputs).length;
        } catch (Exception ignored) {
            return estimatedNodeOutputSize(outputs);
        }
    }

    private int estimatedNodeOutputSize(Map<String, Map<String, Object>> outputs) {
        long total = 0;
        for (Map<String, Object> output : outputs.values()) {
            for (Map.Entry<String, Object> entry : output.entrySet()) {
                total += entry.getKey().length()
                        + (entry.getValue() != null ? entry.getValue().toString().length() : 4);
                if (total > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return (int) total;
    }

    /** 是否超过 1MB 上限 */
    @JsonIgnore
    public boolean isOversized() { return approxSizeBytes.get() > maxSizeBytes; }

    @JsonIgnore
    public boolean isNearSizeLimit() { return approxSizeBytes.get() > warnSizeBytes; }

    /** 获取估算大小（字节） */
    @JsonIgnore
    public int getApproxSizeBytes() { return approxSizeBytes.get(); }

    public int calculateSerializedContextSize() {
        synchronized (outputLock) {
            int size = currentNodeOutputSizeLocked();
            approxSizeBytes.set(size);
            return size;
        }
    }

    public void rebuildDerivedState() {
        synchronized (outputLock) {
            rebuildDerivedStateLocked();
        }
    }

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
        // 节点输出优先于触发载荷，允许下游节点读取上游加工后的同名字段。
        if (val != null) {
            return val;
        }
        Object legacyNodeOutput = legacyNodeOutputValue(fieldKey);
        if (legacyNodeOutput != null) {
            return legacyNodeOutput;
        }
        Object latestOutput = latestOutputValues.get(fieldKey);
        if (latestOutput != null) {
            return latestOutput;
        }
        Object legacyLatestOutput = legacyLatestOutputValue(fieldKey);
        return legacyLatestOutput != null ? legacyLatestOutput : triggerPayload.get(fieldKey);
    }

    public void setContextValue(String fieldKey, Object value) {
        Objects.requireNonNull(fieldKey, "fieldKey must not be null");
        if (value == null) {
            flatContext.remove(fieldKey);
            flatKeyOwners.remove(fieldKey);
            return;
        }
        flatContext.put(fieldKey, value);
        flatKeyOwners.put(fieldKey, "__manual__");
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
                || s == NodeStatus.SKIPPED;
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
                status == NodeStatus.FAILED || status == NodeStatus.TIMEOUT
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
                || s == NodeStatus.SKIPPED;
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

    private void fitUnderLimit(LinkedHashMap<String, Map<String, Object>> candidate) {
        while (serializedContextSize(triggerPayload, candidate) > maxSizeBytes && candidate.size() > 1) {
            String oldestNodeId = candidate.keySet().iterator().next();
            candidate.remove(oldestNodeId);
        }
        int candidateSize = serializedContextSize(triggerPayload, candidate);
        if (candidateSize > maxSizeBytes) {
            throw contextOverflow(candidateSize);
        }
    }

    private ContextOverflowException contextOverflow(int candidateSize) {
        return new ContextOverflowException(
                "execution context exceeds max bytes (1MB default): " + candidateSize + " > " + maxSizeBytes,
                candidateSize);
    }

    private Object legacyNodeOutputValue(String fieldKey) {
        if (fieldKey == null) {
            return null;
        }
        int separator = fieldKey.indexOf('.');
        if (separator <= 0 || separator == fieldKey.length() - 1) {
            return null;
        }
        String nodeId = fieldKey.substring(0, separator);
        String outputKey = fieldKey.substring(separator + 1);
        synchronized (outputLock) {
            Map<String, Object> output = nodeOutputs.get(nodeId);
            return output == null ? null : output.get(outputKey);
        }
    }

    private Object legacyLatestOutputValue(String fieldKey) {
        if (fieldKey == null) {
            return null;
        }
        synchronized (outputLock) {
            List<Map<String, Object>> outputs = new ArrayList<>(nodeOutputs.values());
            ListIterator<Map<String, Object>> iterator = outputs.listIterator(outputs.size());
            while (iterator.hasPrevious()) {
                Object value = iterator.previous().get(fieldKey);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private int serializedContextSize(Map<String, Object> payload, Map<String, Map<String, Object>> outputs) {
        int payloadSize = payload == null || payload.isEmpty() ? 0 : serializedObjectSize(payload);
        return payloadSize + serializedNodeOutputSize(outputs);
    }

    private int serializedObjectSize(Object value) {
        try {
            return SIZE_OBJECT_MAPPER.writeValueAsBytes(value).length;
        } catch (Exception ignored) {
            return value == null ? 4 : value.toString().length();
        }
    }

    private void rebuildDerivedStateLocked() {
        flatContext.clear();
        flatKeyOwners.clear();
        nodeFlatKeys.clear();
        latestOutputValues.clear();
        latestOutputOwners.clear();
        nodeOutputs.forEach((nodeId, output) -> {
            Set<String> writtenFlatKeys = new HashSet<>();
            output.forEach((key, value) -> {
                if (value != null) {
                    putOwnedFlatKey(nodeId, qualifiedOutputKey(nodeId, key), value, writtenFlatKeys);
                    latestOutputValues.put(key, value);
                    latestOutputOwners.put(key, nodeId);
                }
            });
            if (!writtenFlatKeys.isEmpty()) {
                nodeFlatKeys.put(nodeId, writtenFlatKeys);
            }
        });
        approxSizeBytes.set(currentNodeOutputSizeLocked());
    }

    private Map<String, Object> mutableDeepCopyMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                snapshot.put(key, deepCopyValue(value));
            }
        });
        return snapshot;
    }

    private Map<String, Object> readOnlyMapSnapshot(Map<String, Object> source) {
        return Collections.unmodifiableMap(mutableDeepCopyMap(source));
    }

    private Object deepCopyValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> {
                if (key != null) {
                    copy.put(String.valueOf(key), deepCopyValue(nestedValue));
                }
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> copy = collection.stream()
                    .map(this::deepCopyValue)
                    .toList();
            return Collections.unmodifiableList(new ArrayList<>(copy));
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> copy = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                copy.add(deepCopyValue(Array.get(value, i)));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
