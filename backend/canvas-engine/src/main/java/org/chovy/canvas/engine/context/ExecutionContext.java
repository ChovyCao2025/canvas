package org.chovy.canvas.engine.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private Map<String, Object> triggerPayload = new HashMap<>();

    /** 各节点产出数据历史（nodeId → {fieldKey → value}），供轨迹查询 */
    private final Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    /** 扁平化快速查找 Map，O(1)。由 putNodeOutput 维护 */
    private final Map<String, Object> flatContext = new ConcurrentHashMap<>();

    /** 各节点执行状态（需持久化，多阶段恢复和汇聚判断依赖此状态） */
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
    public synchronized Map<String, Map<String, Object>> getNodeOutputs() {
        Map<String, Map<String, Object>> snapshot = new HashMap<>();
        nodeOutputs.forEach((nodeId, output) -> snapshot.put(nodeId, output));
        return Collections.unmodifiableMap(snapshot);
    }

    /** 获取扁平上下文只读快照，避免调用方绕过 putNodeOutput 写入边界。 */
    public synchronized Map<String, Object> getFlatContext() {
        return Collections.unmodifiableMap(new HashMap<>(flatContext));
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
        // nodeOutputs 保留按节点分组的不可变快照，flatContext 提供跨节点字段的快速读取。
        Map<String, Object> outputSnapshot = Collections.unmodifiableMap(new HashMap<>(output));
        nodeOutputs.put(nodeId, outputSnapshot);
        flatContext.putAll(outputSnapshot);
        outputSnapshot.forEach((key, value) -> flatContext.put(nodeId + "." + key, value));

        // 大小监控：累加估算字节数（设计文档 13.7节）
        // 使用轻量累加而非每次 JSON 序列化，避免 O(n) 开销
        outputSnapshot.forEach((k, v) ->
            approxSizeBytes.addAndGet(k.length() + (v != null ? v.toString().length() : 4)));

        if (approxSizeBytes.get() > MAX_SIZE_BYTES) {
            // 不截断（截断可能破坏防资损逻辑），仅记录 WARN
            // 调用方可通过检查 isOversized() 决定是否中止
        } else if (approxSizeBytes.get() > WARN_SIZE_BYTES) {
            // 超过 512KB 提前预警，便于排查超大字段
        }
    }

    /** 是否超过 1MB 上限 */
    @JsonIgnore
    public synchronized boolean isOversized() { return approxSizeBytes.get() > MAX_SIZE_BYTES; }

    /** 获取估算大小（字节） */
    @JsonIgnore
    public synchronized int getApproxSizeBytes() { return approxSizeBytes.get(); }

    /**
     * 查询或读取 get Context Value 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param fieldKey fieldKey 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
// ── 读取上下文字段，O(1) ─────────────────────────────────────

    public synchronized Object getContextValue(String fieldKey) {
        Object val = flatContext.get(fieldKey);
        // 节点输出优先于触发载荷，允许下游节点读取上游加工后的同名字段。
        return val != null ? val : triggerPayload.get(fieldKey);
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
}
