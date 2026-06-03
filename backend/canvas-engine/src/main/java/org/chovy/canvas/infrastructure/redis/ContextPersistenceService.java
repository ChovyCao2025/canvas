package org.chovy.canvas.infrastructure.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExecutionContext 在 Redis 中的持久化（多阶段执行挂起/恢复）。
 *
 * <p>所有 key 统一通过 {@link RedisKeyUtil} 构造（支持命名空间前缀），
 * 不再硬编码 "canvas:" 前缀，以保证 {@code canvas.redis.key-prefix} 配置全局生效。
 *
 * <p>设计目标：
 * 1) 支持节点挂起后的上下文恢复；
 * 2) 支持恢复过程并发互斥（resumeLock）；
 * 3) 支持消息级幂等去重（dedupKey）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPersistenceService {

    /** 阻塞式 Redis 模板（执行链路中以轻量 KV 操作为主）。 */
    private final StringRedisTemplate redis;

    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;

    /**
     * 统一 key 构造器，所有 key 通过此类生成，保证命名空间前缀一致。
     * Fix 3：原实现硬编码 "canvas:" 前缀，改为走 RedisKeyUtil 统一管理。
     */
    private final RedisKeyUtil keys;

    /** 执行轨迹写入缓冲区，用于记录上下文持久化关键事件。 */
    private final TraceWriteBuffer traceBuffer;

    /** 上下文快照过期时间（秒），默认 24 小时。 */
    @Value("${canvas.execution.context-ttl-sec:86400}")
    private long ttlSec;

    public record NodeState(NodeStatus status, Map<String, Object> output) {
    }

    /**
     * 创建或新增 save 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     */
// ── 上下文持久化 ──────────────────────────────────────────────

    /** 保存或覆盖上下文快照，并刷新 TTL。 */
    public void save(ExecutionContext ctx) {
        try {
            String json = objectMapper.writeValueAsString(ctx);
            // 覆盖写 + TTL 续期：每次挂起/恢复都会刷新上下文生命周期
            redis.opsForValue().set(keys.context(ctx.getCanvasId(), ctx.getUserId()),
                    json, Duration.ofSeconds(ttlSec));
            emitContextTrace(ctx, "CONTEXT_SAVE");
        } catch (Exception e) {
            log.error("保存 ExecutionContext 失败: {}", e.getMessage());
        }
    }

    /** 加载上下文；不存在或反序列化失败时返回 null。 */
    public ExecutionContext load(Long canvasId, String userId) {
        String json = redis.opsForValue().get(keys.context(canvasId, userId));
        if (json == null) return null;
        try {
            // 恢复执行直接反序列化完整上下文，保持挂起前的节点状态和用户变量。
            ExecutionContext ctx = objectMapper.readValue(json, ExecutionContext.class);
            ctx.rebuildDerivedState();
            emitContextTrace(ctx, "CONTEXT_LOAD");
            return ctx;
        } catch (Exception e) {
            log.error("反序列化 ExecutionContext 失败: {}", e.getMessage());
            return null;
        }
    }

    /** 删除上下文快照（执行结束或失败后清理）。 */
    public void delete(Long canvasId, String userId) {
        redis.delete(keys.context(canvasId, userId));
    }

    /** 判断上下文是否存在（常用于恢复分支判断）。 */
    public boolean exists(Long canvasId, String userId) {
        return redis.hasKey(keys.context(canvasId, userId));
    }

    /** 增量保存单节点状态和输出；失败只记录日志，不阻断 DAG 执行。 */
    public void saveNodeState(String executionId, String nodeId, NodeStatus status, Map<String, Object> output) {
        try {
            String key = keys.nodeState(executionId, nodeId);
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("status", status.name());
            fields.put("output", objectMapper.writeValueAsString(output == null ? Map.of() : output));
            redis.opsForHash().putAll(key, fields);
            redis.expire(key, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.warn("[CTX] 保存节点增量状态失败 executionId={} nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
        }
    }

    /** 加载单节点增量状态；不存在或状态不可解析时返回 null。 */
    public NodeState loadNodeState(String executionId, String nodeId) {
        try {
            Map<Object, Object> fields = redis.opsForHash().entries(keys.nodeState(executionId, nodeId));
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            Object rawStatus = fields.get("status");
            if (rawStatus == null) {
                return null;
            }
            NodeStatus status = NodeStatus.valueOf(rawStatus.toString());
            return new NodeState(status, parseNodeOutput(executionId, nodeId, fields.get("output")));
        } catch (Exception e) {
            log.warn("[CTX] 加载节点增量状态失败 executionId={} nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
            return null;
        }
    }

    /** 批量加载指定节点状态，供后续恢复逻辑按需使用。 */
    public Map<String, NodeState> loadNodeStates(String executionId, Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Map.of();
        }
        Map<String, NodeState> states = new LinkedHashMap<>();
        for (String nodeId : nodeIds) {
            NodeState state = loadNodeState(executionId, nodeId);
            if (state != null) {
                states.put(nodeId, state);
            }
        }
        return states;
    }

    private Map<String, Object> parseNodeOutput(String executionId, String nodeId, Object rawOutput) {
        if (rawOutput == null || rawOutput.toString().isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> output = objectMapper.readValue(rawOutput.toString(),
                    new TypeReference<Map<String, Object>>() {
                    });
            return output == null ? Map.of() : output;
        } catch (Exception e) {
            log.warn("[CTX] 节点输出 JSON 解析失败 executionId={} nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 执行 acquire Resume Lock 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param instanceId instanceId 对应的业务主键或标识
     * @param timeoutSec timeoutSec 时间、过期时间或持续时长参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
// ── 恢复锁 ─────────────────────────────────────────────────────

    /**
     * 尝试获取 resumeLock（分布式互斥锁，跨机安全）。
     *
     * <p>使用 Redis SETNX + EX，原子操作，保证同一时刻只有一个 JVM 实例
     * 可以持有 (canvasId, userId) 的恢复锁。
     * 锁 TTL = globalTimeoutSec，等于画布最大执行超时，保证机器崩溃后锁自动释放。
     *
     * @param instanceId 当前实例标识（UUID），记录锁的持有者，供原子释放时验证
     * @return true=获锁成功，false=已有其他实例持有锁（放弃本次触发）
     */
    public boolean acquireResumeLock(Long canvasId, String userId, String instanceId,
                                      long timeoutSec) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(keys.resumeLock(canvasId, userId),
                        instanceId, Duration.ofSeconds(timeoutSec)));
    }

    /**
     * 原子释放 resumeLock（Lua check-then-del，跨机安全）。
     *
     * <p>只有当锁的 value 等于 {@code token} 时才执行 DEL，防止锁过期后被其他机器重新获取，
     * 而本机因执行延迟才来释放，错误删除他机的锁。
     *
     * @param token 获锁时传入的 instanceId，null 时降级为无校验 DEL（兜底）
     */
    public void releaseResumeLock(Long canvasId, String userId, String token) {
        String key = keys.resumeLock(canvasId, userId);
        if (token == null) {
            // 历史调用未传 token 时只能降级直接删除；新路径应优先走 Lua 校验释放。
            redis.delete(key);
            return;
        }
        try {
            redis.execute(RESUME_LOCK_RELEASE_SCRIPT, List.of(key), token);
        } catch (Exception e) {
            log.warn("[CTX] resumeLock Lua 释放失败，降级 DEL key={}: {}", key, e.getMessage());
            redis.delete(key);
        }
    }

    /** 释放 WAIT 恢复锁的 Lua 脚本，保证只释放当前持有者的锁。 */
    private static final RedisScript<Long> RESUME_LOCK_RELEASE_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    /**
     * 执行 acquire Dedup 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param msgId msgId 对应的业务主键或标识
     * @param ttl ttl 时间、过期时间或持续时长参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
// ── dedup key ────────────────────────────────────────────────

    /** 尝试写入消息级幂等 key，成功表示本次消息可继续执行。 */
    public boolean acquireDedup(Long canvasId, String userId, String msgId, Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(keys.dedup(canvasId, userId, msgId), "1", ttl));
    }

    /** 主动释放 dedup key；常规场景依赖 TTL 自然过期。 */
    public void releaseDedup(String fullDedupKey) {
        redis.delete(fullDedupKey);
    }

    /** 构造完整 dedup key，供调用方持有以便后续释放或可观测。 */
    public String buildDedupKey(Long canvasId, String userId, String msgId) {
        return keys.dedup(canvasId, userId, msgId);
    }

    private void emitContextTrace(ExecutionContext ctx, String nodeId) {
        try {
            CanvasExecutionTraceDO trace = CanvasExecutionTraceDO.builder()
                    .executionId(ctx.getExecutionId())
                    .nodeId(nodeId)
                    .nodeType("SYSTEM")
                    .status(1)
                    .startedAt(LocalDateTime.now())
                    .build();
            traceBuffer.addTrace(trace, true);
        } catch (Exception e) {
            log.warn("[CTX] critical context trace emission failed nodeId={} executionId={}: {}",
                    nodeId, ctx.getExecutionId(), e.getMessage());
        }
    }
}
