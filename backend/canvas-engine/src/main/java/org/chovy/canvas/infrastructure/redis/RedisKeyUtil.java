package org.chovy.canvas.infrastructure.redis;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis key 统一构造（设计文档 25.3.1节）。
 *
 * 所有 Redis key 通过此类构造，支持命名空间前缀配置，
 * 解决多环境共用同一 Redis 时的 key 冲突问题。
 *
 * 配置：canvas.redis.key-prefix=canvas（默认）
 */
@Component
public class RedisKeyUtil {

    /** Redis key 命名空间前缀，用于隔离不同环境或应用实例。 */
    @Getter
    @Value("${canvas.redis.key-prefix:canvas}")
    private String prefix = "canvas";

    /**
     * 执行 trigger Mq 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param topicKey topicKey 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
// ── 触发路由 ─────────────────────────────────────────────────
    /**
     * triggerMq 创建或触发 infrastructure.redis 场景的业务处理。
     * @param topicKey 业务键，用于在同一租户下定位资源。
     * @return 返回 trigger mq 生成的文本或业务键。
     */
    public String triggerMq(String topicKey)       { return prefix + ":trigger:mq:" + topicKey; }
    /**
     * 执行 trigger Behavior 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param code code 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    public String triggerBehavior(String code)     { return prefix + ":trigger:behavior:" + code; }
    /**
     * 执行 trigger Tagger 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param tagCodeKey tagCodeKey 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    public String triggerTagger(String tagCodeKey) { return prefix + ":trigger:tagger:" + tagCodeKey; }
    /**
     * 执行 trigger Mq Pattern 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String triggerMqPattern()               { return prefix + ":trigger:mq:*"; }
    /** 行为事件触发路由 key 扫描模式。 */
    public String triggerBehaviorPattern()         { return prefix + ":trigger:behavior:*"; }
    /** Tagger 实时触发路由 key 扫描模式。 */
    public String triggerTaggerPattern()           { return prefix + ":trigger:tagger:*"; }
    /**
     * 执行 trigger Pattern 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String triggerPattern()                 { return prefix + ":trigger:*"; }
    /**
     * 执行 trigger Route Ready 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String triggerRouteReady()              { return prefix + ":trigger:routes:ready"; }
    /**
     * 执行 trigger Route Mutation Lock 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String triggerRouteMutationLock()       { return prefix + ":trigger:routes:mutation-lock"; }

    /**
     * 执行 context 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
// ── 执行上下文 ─────────────────────────────────────────────────
    /**
     * context 处理 infrastructure.redis 场景的业务逻辑。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 context 生成的文本或业务键。
     */
    public String context(Long canvasId, String userId)  { return prefix + ":" + canvasId + ":user:" + userId; }
    /**
     * nodeState 处理 infrastructure.redis 场景的业务逻辑。
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @return 返回 node state 生成的文本或业务键。
     */
    public String nodeState(String executionId, String nodeId) {
        return prefix + ":node-state:" + executionId + ":" + nodeId;
    }

    /** 节点级增量状态索引：canvas:node-state-index:{executionId}。 */
    public String nodeStateIndex(String executionId) {
        return prefix + ":node-state-index:" + executionId;
    }

    /** 节点级重入 reset marker：canvas:node-state-reset:{executionId}。 */
    public String nodeStateResetIndex(String executionId) {
        return prefix + ":node-state-reset:" + executionId;
    }

    /** 节点执行门控 key：canvas:gate:{executionId}:{nodeId}。 */
    public String gate(String executionId, String nodeId) {
        return prefix + ":gate:" + executionId + ":" + nodeId;
    }

    /** 节点执行 repeat 信号 key：canvas:gate-repeat:{executionId}:{nodeId}。 */
    public String gateRepeat(String executionId, String nodeId) {
        return prefix + ":gate-repeat:" + executionId + ":" + nodeId;
    }

    /** 特殊节点超时延迟队列：canvas:delay-queue。 */
    public String delayQueue() {
        return prefix + ":delay-queue";
    }

    /** 特殊节点超时处理中队列：canvas:delay-queue:inflight。 */
    public String delayQueueInflight() {
        return prefix + ":delay-queue:inflight";
    }

    /**
     * 执行 resume Lock 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    public String resumeLock(Long canvasId, String userId){ return prefix + ":resume-lock:" + canvasId + ":" + userId; }
    /**
     * 执行 dedup 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param msgId msgId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    public String dedup(Long canvasId, String userId, String msgId) {
        return prefix + ":dedup:" + canvasId + ":" + userId + ":" + msgId;
    }
    /**
     * 执行 global Count 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
    public String globalCount(Long canvasId)  { return prefix + ":global_count:" + canvasId; }
    /**
     * 执行 quota 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param userId userId 对应的业务主键或标识
     * @param date date 时间、过期时间或持续时长参数
     * @return 转换或查询得到的字符串结果
     */
    public String quota(Long canvasId, String userId, String date) {
        return prefix + ":quota:" + canvasId + ":" + userId + ":" + date;
    }
    /**
     * 执行 api Rate Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param apiKey apiKey 对应的缓存键、配置键或业务键
     * @param epochSecond epochSecond 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    public String apiRateLimit(String apiKey, long epochSecond) {
        return prefix + ":ratelimit:" + apiKey + ":" + epochSecond;
    }
    /**
     * 执行 execution Request Replay Rate Limit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param scope scope 方法执行所需的业务参数
     * @param operator operator 操作人标识
     * @param epochMinute epochMinute 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    public String executionRequestReplayRateLimit(String scope, String operator, long epochMinute) {
        String normalizedOperator = operator == null || operator.isBlank() ? "system" : operator;
        return prefix + ":execution-request:replay:" + scope + ":" + normalizedOperator + ":" + epochMinute;
    }

    /**
     * 发布或发送 publish Lock 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
// ── 并发锁 ─────────────────────────────────────────────────────
    /**
     * publishLock 创建或触发 infrastructure.redis 场景的业务处理。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    public String publishLock(Long canvasId) { return prefix + ":publish:lock:" + canvasId; }

    /**
     * 执行 inflight Canvas 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
// ── 分布式并发注册表（InFlightExecutionRegistry）────────────────
    /** 每个画布当前正在执行的任务集合（ZSET，score=过期时间戳ms）。 */
    public String inflightCanvas(Long canvasId) { return prefix + ":inflight:canvas:" + canvasId; }
    /** 每个 execution lane 当前正在执行的任务集合（ZSET，score=过期时间戳ms）。 */
    public String inflightLane(org.chovy.canvas.engine.lane.ExecutionLane lane) {
        return prefix + ":inflight:lane:" + lane.key();
    }
    /** 全局正在执行的任务集合（ZSET，score=过期时间戳ms）。 */
    public String inflightGlobal()              { return prefix + ":inflight:global"; }
    /**
     * 集群 globalMaxConcurrency 基准值（String），用于启动时一致性校验。
     * 首台实例 SETNX 写入，后续实例读取并与本地配置比对；不一致则 fail-fast。
     */
    public String globalMaxConcurrencyConfig()  { return prefix + ":config:max-concurrency"; }

    /**
     * 执行 event Dedup 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param idempotencyKey idempotencyKey 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
// ── 事件上报幂等 ──────────────────────────────────────────────
    /** 事件级幂等 key，按 idempotencyKey 去重，TTL 24h。 */
    public String eventDedup(String idempotencyKey) { return prefix + ":event:dedup:" + idempotencyKey; }

    /**
     * 执行 login Fail 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param username username 用户或客户相关标识/数据
     * @return 转换或查询得到的字符串结果
     */
// ── 认证安全 ───────────────────────────────────────────────────
    /**
     * loginFail 处理 infrastructure.redis 场景的业务逻辑。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 login fail 生成的文本或业务键。
     */
    public String loginFail(String username)   { return prefix + ":login:fail:" + username; }
    /**
     * 执行 login Locked 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param username username 用户或客户相关标识/数据
     * @return 转换或查询得到的字符串结果
     */
    public String loginLocked(String username) { return prefix + ":login:locked:" + username; }
    /**
     * 执行 jwt Revoked 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param tokenHash tokenHash 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    public String jwtRevoked(String tokenHash) { return prefix + ":jwt:revoked:" + tokenHash; }

    /**
     * 判断 canvas Config 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param versionId versionId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
// ── 缓存 ───────────────────────────────────────────────────────
    /**
     * canvasConfig 校验或转换 infrastructure.redis 场景的数据。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param versionId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    public String canvasConfig(Long canvasId, Long versionId) {
        return prefix + ":" + canvasId + ":v" + versionId + ":config";
    }
    /**
     * 执行 cache Invalidate Channel 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String cacheInvalidateChannel() { return prefix + ":cache:invalidate"; }

    /**
     * 执行 kill Channel 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 转换或查询得到的字符串结果
     */
// ── Kill Switch ────────────────────────────────────────────────
    /**
     * killChannel 处理 infrastructure.redis 场景的业务逻辑。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 kill channel 生成的文本或业务键。
     */
    public String killChannel(Long canvasId)    { return prefix + ":kill:" + canvasId; }
    /**
     * 执行 kill Pattern 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String killPattern()                 { return prefix + ":kill:*"; }

    /**
     * 执行 notification Ws Ticket 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ticket ticket 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
// ── 消息中心 ───────────────────────────────────────────────────
    /**
     * notificationWsTicket 处理 infrastructure.redis 场景的业务逻辑。
     * @param ticket ticket 参数，用于 notificationWsTicket 流程中的校验、计算或对象转换。
     * @return 返回 notification ws ticket 生成的文本或业务键。
     */
    public String notificationWsTicket(String ticket) { return prefix + ":notification:ws-ticket:" + ticket; }
    /**
     * 执行 notification Channel 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    public String notificationChannel()              { return prefix + ":notification:events"; }
}
