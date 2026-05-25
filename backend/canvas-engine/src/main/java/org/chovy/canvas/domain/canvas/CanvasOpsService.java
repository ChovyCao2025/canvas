package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
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

    private final CanvasMapper        canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final TriggerRouteService triggerRouteService;
    private final TriggerPreCheckService preCheckService;
    private final CanvasTransactionService canvasTransactionService;
    private final CanvasService canvasService;
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
        // 清理触发路由、调度任务、缓存、配额
        canvasService.applyKillExternalCleanup(id, publishedVersionId);
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
        copy.setSourceTemplateKey(null);
        canvasMapper.insert(copy);

        if (srcDraft != null) {
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

    // ── helpers ──────────────────────────────────────────────────

    private CanvasDO require(Long id) {
        CanvasDO c = canvasMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("画布不存在: " + id);
        return c;
    }

    private CanvasVersionDO latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .eq(CanvasVersionDO::getStatus, 0)
                        .orderByDesc(CanvasVersionDO::getVersion).last("LIMIT 1"));
    }

    private int nextVer(Long canvasId) {
        CanvasVersionDO max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion).last("LIMIT 1"));
        return max != null ? max.getVersion() + 1 : 1;
    }

    private void clearRoutes(Long id, CanvasDO canvas) {
        // 已由 canvasService.applyKillExternalCleanup 统一实现，保留签名兼容旧调用
    }
}
