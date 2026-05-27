package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * 画布运营管控服务：Kill / 灰度发布 / 版本回滚 / 克隆 / 乐观锁保存。
 */
@Service
@RequiredArgsConstructor
public class CanvasOpsService {

    /** 画布 Mapper，用于读取和更新画布主表状态。 */
    private final CanvasMapper        canvasMapper;
    /** 画布版本 Mapper。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** 执行记录 Mapper，用于 FORCE kill 时终止运行中记录。 */
    private final CanvasExecutionMapper executionMapper;
    /** 执行请求 Mapper，用于 FORCE kill 时终止持久请求队列状态。 */
    private final CanvasExecutionRequestMapper executionRequestMapper;
    /** 触发路由服务，负责 Redis 路由注册、清理和查询。 */
    private final TriggerRouteService triggerRouteService;
    /** 触发预检服务，负责有效期、配额、冷却期和并发保护校验。 */
    private final TriggerPreCheckService preCheckService;
    /** 画布事务服务，封装只涉及数据库写入的事务边界。 */
    private final CanvasTransactionService canvasTransactionService;
    /** 画布主服务，用于复用发布、回滚和外部状态清理能力。 */
    private final CanvasService canvasService;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final StringRedisTemplate redis;

    /**
     * 带乐观锁更新画布草稿信息
     * @param id 画布 ID
     * @param name 画布名称
     * @param description 描述
     * @param graphJson 图表 JSON
     * @param editVersion 当前编辑版本号
     * @param operator 操作人
     */
    @Transactional
    public void saveWithOptimisticLock(Long id, String name, String description,
                                        String graphJson, int editVersion, String operator) {
        // CAS 更新 edit_version
        int updated = canvasMapper.updateEditVersion(id, editVersion, editVersion + 1, name, description);
        if (updated == 0) throw new IllegalStateException("CANVAS_010");  // 409 冲突

        // graphJson 为空时仅更新元数据（名称/描述）
        if (graphJson != null) {
            CanvasVersionDO draft = latestDraft(id);
            if (draft != null) {
                draft.setGraphJson(graphJson);
                canvasVersionMapper.updateById(draft);
            } else {
                CanvasVersionDO v = new CanvasVersionDO();
                v.setCanvasId(id); v.setVersion(1); v.setGraphJson(graphJson);
                v.setStatus(VersionStatus.DRAFT.getCode()); v.setCreatedBy(operator);
                canvasVersionMapper.insert(v);
            }
        }
    }

// ── Kill Switch ────────────────────────────────────────────────

    /**
     * 紧急终止画布。
     *
     * <p>两阶段执行（与 offline 同模式）：
     * <ol>
     *   <li>{@link CanvasTransactionService#killDb} — 事务内 DB 操作：置 KILLED、清 publishedVersionId；</li>
     *   <li>事务外：广播 Kill 信号 → 路由/调度/缓存清理 → 配额清理。</li>
     * </ol>
     * 外部副作用失败不会回滚 DB，路由/缓存最终会通过 TTL 或下次操作自愈。
     */
    public void kill(Long id, String mode) {
        // Step 1: 事务内 DB 操作，返回下线前的 publishedVersionId 供外部清理使用
        Long publishedVersionId = canvasTransactionService.killDb(id);

        // Step 2: 事务外副作用
        // 广播 Kill 信号（Phase 11 Redis Pub/Sub），各机器收到后立即取消正在进行的执行
        redis.convertAndSend("canvas:kill:" + id, mode);
        if ("FORCE".equalsIgnoreCase(mode)) {
            // FORCE 模式需要同步落库终止存量 RUNNING 记录，避免执行态长期悬挂。
            markRunningExecutionsFailed(id);
            executionRequestMapper.markForceCancelledByCanvas(id, java.time.LocalDateTime.now());
        }
        // 清理触发路由、调度任务、缓存、配额
        canvasService.applyKillExternalCleanup(id, publishedVersionId);
    }

    /** FORCE Kill 时将该画布仍处于 RUNNING 的执行记录统一标记为失败。 */
    private void markRunningExecutionsFailed(Long canvasId) {
        CanvasExecutionDO update = new CanvasExecutionDO();
        update.setStatus(ExecutionStatus.FAILED.getCode());
        update.setResult("{\"error\":\"FORCE_CANCELLED\"}");
        executionMapper.update(update,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CanvasExecutionDO>()
                        .eq(CanvasExecutionDO::getCanvasId, canvasId)
                        .eq(CanvasExecutionDO::getStatus, ExecutionStatus.RUNNING.getCode()));
    }

// ── 灰度发布 ────────────────────────────────────────────────

    /**
     * 启动画布灰度发布
     * @param id 画布 ID
     * @param percent 流量比例
     * @param operator 操作人
     */
    @Transactional
    public void startCanary(Long id, int percent, String operator) {
        CanvasDO canvas = require(id);
        if (canvas.getStatus() != CanvasStatusEnum.PUBLISHED.getCode()) throw new IllegalStateException("画布未在发布状态");

        // 生成灰度版本快照（复用当前草稿），与正式发布一样保留不可变版本
        CanvasVersionDO draft = latestDraft(id);
        if (draft == null) throw new IllegalStateException("无草稿可灰度");

        CanvasVersionDO canary = new CanvasVersionDO();
        canary.setCanvasId(id);
        canary.setVersion(nextVer(id));
        canary.setGraphJson(draft.getGraphJson());
        canary.setStatus(VersionStatus.PUBLISHED.getCode());
        canary.setCreatedBy(operator);
        canvasVersionMapper.insert(canary);

        canvas.setCanaryVersionId(canary.getId());
        canvas.setCanaryPercent(percent);
        canvasMapper.updateById(canvas);
    }

    /**
     * 将灰度版本晋升为正式版本
     * @param id 画布 ID
     */
    @Transactional
    public void promoteCanary(Long id) {
        CanvasDO canvas = require(id);
        if (canvas.getCanaryVersionId() == null) throw new IllegalStateException("无灰度版本");

        canvas.setPreviousVersionId(canvas.getPublishedVersionId());
        canvas.setPublishedVersionId(canvas.getCanaryVersionId());
        canvas.setCanaryVersionId(null);
        canvas.setCanaryPercent(null);
        canvasMapper.updateById(canvas);
    }

    /**
     * 回滚灰度发布
     * @param id 画布 ID
     */
    @Transactional
    public void rollbackCanary(Long id) {
        CanvasDO canvas = require(id);
        canvas.setCanaryVersionId(null);
        canvas.setCanaryPercent(null);
        canvasMapper.updateById(canvas);
    }

    /**
     * 执行 rollback 对应的业务逻辑。
     *
     * <p>该方法在事务边界内执行，确保相关数据库写入保持一致。
     *
     * @param id id 对应的业务主键或标识
     */
// ── 版本回滚 ────────────────────────────────────────────────

    /**
     * 回滚画布到上一个版本
     * @param id 画布 ID
     */
    @Transactional
    public void rollback(Long id) {
        CanvasDO canvas = require(id);
        if (canvas.getPreviousVersionId() == null) throw new IllegalStateException("无上一版本可回滚");

        Long tmp = canvas.getPublishedVersionId();
        canvas.setPublishedVersionId(canvas.getPreviousVersionId());
        canvas.setPreviousVersionId(tmp);
        canvasMapper.updateById(canvas);
    }

// ── 克隆 ────────────────────────────────────────────────────

    /**
     * 克隆画布
     * @param id 原画布 ID
     * @param operator 操作人
     * @return 新的画布对象
     */
    @Transactional
    public CanvasDO clone(Long id, String operator) {
        CanvasDO src = require(id);
        CanvasVersionDO srcDraft = latestDraft(id);

        CanvasDO copy = new CanvasDO();
        copy.setName(src.getName() + " (副本)");
        copy.setDescription(src.getDescription());
        copy.setStatus(CanvasStatusEnum.DRAFT.getCode());
        copy.setCreatedBy(operator);
        copy.setIsExample(0);
        // 克隆结果必须脱离示例模板来源，避免后续示例同步把用户副本识别为官方示例。
        copy.setSourceTemplateKey(null);
        canvasMapper.insert(copy);

        if (srcDraft != null) {
            // 只克隆最新草稿为新画布的草稿版本，不继承发布态、灰度和外部路由状态。
            CanvasVersionDO v = new CanvasVersionDO();
            v.setCanvasId(copy.getId()); v.setVersion(1);
            v.setGraphJson(srcDraft.getGraphJson());
            v.setStatus(VersionStatus.DRAFT.getCode()); v.setCreatedBy(operator);
            canvasVersionMapper.insert(v);
        }

        return copy;
    }

// ── 版本对比（设计文档 23.3节）─────────────────────────────────

    /**
     * 比较两个画布版本之间的配置差异
     * @param canvasId 画布 ID
     * @param v1Id 版本 ID 1
     * @param v2Id 版本 ID 2
     * @return 差异信息
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> diff(Long canvasId, Long v1Id, Long v2Id) {

        CanvasVersionDO v1 = canvasVersionMapper.selectById(v1Id);
        CanvasVersionDO v2 = canvasVersionMapper.selectById(v2Id);
        if (v1 == null || v2 == null) throw new IllegalArgumentException("版本不存在");

        // 解析两个版本的节点列表
        java.util.List<java.util.Map<String, Object>> nodes1 = parseNodes(v1.getGraphJson());
        java.util.List<java.util.Map<String, Object>> nodes2 = parseNodes(v2.getGraphJson());

        java.util.Map<String, java.util.Map<String, Object>> map1 =
                nodes1.stream().collect(java.util.stream.Collectors.toMap(
                        n -> (String) n.get("id"), n -> n));
        java.util.Map<String, java.util.Map<String, Object>> map2 =
                nodes2.stream().collect(java.util.stream.Collectors.toMap(
                        n -> (String) n.get("id"), n -> n));

        // added: 在 v2 有但 v1 没有的节点
        java.util.List<Object> added = nodes2.stream()
                .filter(n -> !map1.containsKey(n.get("id")))
                .map(n -> java.util.Map.of(
                        MapFieldKeys.NODE_ID, n.get("id"),
                        MapFieldKeys.TYPE, n.get("type"),
                        MapFieldKeys.NAME, n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        // removed: 在 v1 有但 v2 没有的节点
        java.util.List<Object> removed = nodes1.stream()
                .filter(n -> !map2.containsKey(n.get("id")))
                .map(n -> java.util.Map.of(
                        MapFieldKeys.NODE_ID, n.get("id"),
                        MapFieldKeys.TYPE, n.get("type"),
                        MapFieldKeys.NAME, n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        // modified: 两个版本都有但配置或节点名称不同的节点
        java.util.List<Object> modified = nodes2.stream()
                .filter(n -> map1.containsKey(n.get("id")))
                .filter(n -> {
                    java.util.Map<String, Object> old = map1.get(n.get("id"));
                    return !java.util.Objects.equals(n.get("config"), old.get("config")) ||
                           !java.util.Objects.equals(n.get("name"), old.get("name"));
                })
                .map(n -> java.util.Map.of(
                        MapFieldKeys.NODE_ID, n.get("id"),
                        MapFieldKeys.TYPE, n.get("type"),
                        MapFieldKeys.NAME, n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        return java.util.Map.of(
                MapFieldKeys.V1, java.util.Map.of(MapFieldKeys.VERSION_ID, v1Id, MapFieldKeys.VERSION, v1.getVersion()),
                MapFieldKeys.V2, java.util.Map.of(MapFieldKeys.VERSION_ID, v2Id, MapFieldKeys.VERSION, v2.getVersion()),
                MapFieldKeys.ADDED, added,
                MapFieldKeys.REMOVED, removed,
                MapFieldKeys.MODIFIED, modified,
                MapFieldKeys.SUMMARY, java.util.Map.of(
                        MapFieldKeys.ADDED_COUNT, added.size(),
                        MapFieldKeys.REMOVED_COUNT, removed.size(),
                        MapFieldKeys.MODIFIED_COUNT, modified.size(),
                        MapFieldKeys.UNCHANGED, nodes2.size() - added.size() - modified.size()
                )
        );
    }

    /** 将版本图 JSON 解析为节点列表，解析失败时按空图处理，避免差异接口中断。 */
    @SuppressWarnings("unchecked")
    private java.util.List<java.util.Map<String, Object>> parseNodes(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) return java.util.List.of();
        try {
            java.util.Map<String, Object> root = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(graphJson, java.util.Map.class);
            Object nodes = root.get("nodes");
            if (nodes instanceof java.util.List<?> l) {
                return (java.util.List<java.util.Map<String, Object>>) l;
            }
        } catch (Exception ignored) {}
        return java.util.List.of();
    }

    /**
     * 执行 require 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param id id 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
// ── helpers ──────────────────────────────────────────────────

    /** 按 ID 查询画布，不存在时抛出业务异常。 */
    private CanvasDO require(Long id) {
        CanvasDO c = canvasMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("画布不存在: " + id);
        return c;
    }

    /** 查询指定画布当前版本号最大的草稿版本。 */
    private CanvasVersionDO latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .eq(CanvasVersionDO::getStatus, 0)
                        .orderByDesc(CanvasVersionDO::getVersion).last("LIMIT 1"));
    }

    /** 根据现有最大版本号计算下一次快照版本号。 */
    private int nextVer(Long canvasId) {
        CanvasVersionDO max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion).last("LIMIT 1"));
        return max != null ? max.getVersion() + 1 : 1;
    }

    /** 保留旧版本路由清理签名，实际清理由 CanvasService 统一处理。 */
    private void clearRoutes(Long id, CanvasDO canvas) {
        // 已由 canvasService.applyKillExternalCleanup 统一实现，保留签名兼容旧调用
    }
}
