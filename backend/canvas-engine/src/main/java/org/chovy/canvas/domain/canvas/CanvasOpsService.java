package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 画布运营管控服务：Kill / 灰度发布 / 版本回滚 / 克隆 / 乐观锁保存。
 */
@Service
@RequiredArgsConstructor
public class CanvasOpsService {

    private final CanvasMapper        canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final TriggerRouteService triggerRouteService;
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

        if (graphJson != null) {
            CanvasVersion draft = latestDraft(id);
            if (draft != null) {
                draft.setGraphJson(graphJson);
                canvasVersionMapper.updateById(draft);
            } else {
                CanvasVersion v = new CanvasVersion();
                v.setCanvasId(id); v.setVersion(1); v.setGraphJson(graphJson);
                v.setStatus(VersionStatus.DRAFT.getCode()); v.setCreatedBy(operator);
                canvasVersionMapper.insert(v);
            }
        }
    }

    // ── Kill Switch ────────────────────────────────────────────────

    /**
     * 终止正在运行的画布实例
     * @param id 画布 ID
     * @param mode 终止模式
     */
    @Transactional
    public void kill(Long id, String mode) {
        Canvas canvas = require(id);
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);

        // 广播 Kill 信号（Phase 11 接入 Redis Pub/Sub）
        redis.convertAndSend("canvas:kill:" + id, mode);

        clearRoutes(id, canvas);
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
        Canvas canvas = require(id);
        if (canvas.getStatus() != CanvasStatusEnum.PUBLISHED.getCode()) throw new IllegalStateException("画布未在发布状态");

        // 生成灰度版本快照（复用当前草稿）
        CanvasVersion draft = latestDraft(id);
        if (draft == null) throw new IllegalStateException("无草稿可灰度");

        CanvasVersion canary = new CanvasVersion();
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
        Canvas canvas = require(id);
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
        Canvas canvas = require(id);
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
        Canvas canvas = require(id);
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
    public Canvas clone(Long id, String operator) {
        Canvas src = require(id);
        CanvasVersion srcDraft = latestDraft(id);

        Canvas copy = new Canvas();
        copy.setName(src.getName() + " (副本)");
        copy.setDescription(src.getDescription());
        copy.setStatus(CanvasStatusEnum.DRAFT.getCode());
        copy.setCreatedBy(operator);
        canvasMapper.insert(copy);

        if (srcDraft != null) {
            CanvasVersion v = new CanvasVersion();
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

        CanvasVersion v1 = canvasVersionMapper.selectById(v1Id);
        CanvasVersion v2 = canvasVersionMapper.selectById(v2Id);
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
                .map(n -> java.util.Map.of("nodeId", n.get("id"), "type", n.get("type"), "name", n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        // removed: 在 v1 有但 v2 没有的节点
        java.util.List<Object> removed = nodes1.stream()
                .filter(n -> !map2.containsKey(n.get("id")))
                .map(n -> java.util.Map.of("nodeId", n.get("id"), "type", n.get("type"), "name", n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        // modified: 两个版本都有但配置不同的节点
        java.util.List<Object> modified = nodes2.stream()
                .filter(n -> map1.containsKey(n.get("id")))
                .filter(n -> {
                    java.util.Map<String, Object> old = map1.get(n.get("id"));
                    return !java.util.Objects.equals(n.get("config"), old.get("config")) ||
                           !java.util.Objects.equals(n.get("name"), old.get("name"));
                })
                .map(n -> java.util.Map.of("nodeId", n.get("id"), "type", n.get("type"), "name", n.get("name")))
                .collect(java.util.stream.Collectors.toList());

        return java.util.Map.of(
                "v1", java.util.Map.of("versionId", v1Id, "version", v1.getVersion()),
                "v2", java.util.Map.of("versionId", v2Id, "version", v2.getVersion()),
                "added",    added,
                "removed",  removed,
                "modified", modified,
                "summary", java.util.Map.of(
                        "addedCount",   added.size(),
                        "removedCount", removed.size(),
                        "modifiedCount",modified.size(),
                        "unchanged",    nodes2.size() - added.size() - modified.size()
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

    private Canvas require(Long id) {
        Canvas c = canvasMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("画布不存在: " + id);
        return c;
    }

    private CanvasVersion latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getStatus, 0)
                        .orderByDesc(CanvasVersion::getVersion).last("LIMIT 1"));
    }

    private int nextVer(Long canvasId) {
        CanvasVersion max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersion::getVersion).last("LIMIT 1"));
        return max != null ? max.getVersion() + 1 : 1;
    }

    private void clearRoutes(Long id, Canvas canvas) {
        // 简化：触发路由清理（完整实现在 CanvasService.clearTriggerRoutes）
    }
}
