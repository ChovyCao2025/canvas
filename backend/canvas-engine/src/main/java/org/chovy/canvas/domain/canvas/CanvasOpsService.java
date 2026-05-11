package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    // ── 乐观锁保存草稿 ────────────────────────────────────────────

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
                v.setStatus(0); v.setCreatedBy(operator);
                canvasVersionMapper.insert(v);
            }
        }
    }

    // ── Kill Switch ────────────────────────────────────────────────

    @Transactional
    public void kill(Long id, String mode) {
        Canvas canvas = require(id);
        canvas.setStatus(4); // KILLED（需在前端/DB 映射中补充）
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);

        // 广播 Kill 信号（Phase 11 接入 Redis Pub/Sub）
        redis.convertAndSend("canvas:kill:" + id, mode);

        clearRoutes(id, canvas);
    }

    // ── 灰度发布 ────────────────────────────────────────────────

    @Transactional
    public void startCanary(Long id, int percent, String operator) {
        Canvas canvas = require(id);
        if (canvas.getStatus() != 1) throw new IllegalStateException("画布未在发布状态");

        // 生成灰度版本快照（复用当前草稿）
        CanvasVersion draft = latestDraft(id);
        if (draft == null) throw new IllegalStateException("无草稿可灰度");

        CanvasVersion canary = new CanvasVersion();
        canary.setCanvasId(id);
        canary.setVersion(nextVer(id));
        canary.setGraphJson(draft.getGraphJson());
        canary.setStatus(1);
        canary.setCreatedBy(operator);
        canvasVersionMapper.insert(canary);

        canvas.setCanaryVersionId(canary.getId());
        canvas.setCanaryPercent(percent);
        canvasMapper.updateById(canvas);
    }

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

    @Transactional
    public void rollbackCanary(Long id) {
        Canvas canvas = require(id);
        canvas.setCanaryVersionId(null);
        canvas.setCanaryPercent(null);
        canvasMapper.updateById(canvas);
    }

    // ── 版本回滚 ────────────────────────────────────────────────

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

    @Transactional
    public Canvas clone(Long id, String operator) {
        Canvas src = require(id);
        CanvasVersion srcDraft = latestDraft(id);

        Canvas copy = new Canvas();
        copy.setName(src.getName() + " (副本)");
        copy.setDescription(src.getDescription());
        copy.setStatus(0);
        copy.setCreatedBy(operator);
        canvasMapper.insert(copy);

        if (srcDraft != null) {
            CanvasVersion v = new CanvasVersion();
            v.setCanvasId(copy.getId()); v.setVersion(1);
            v.setGraphJson(srcDraft.getGraphJson());
            v.setStatus(0); v.setCreatedBy(operator);
            canvasVersionMapper.insert(v);
        }

        return copy;
    }

    // ── 版本对比 ────────────────────────────────────────────────

    public java.util.Map<String, Object> diff(Long canvasId, Long v1Id, Long v2Id) {
        CanvasVersion v1 = canvasVersionMapper.selectById(v1Id);
        CanvasVersion v2 = canvasVersionMapper.selectById(v2Id);
        if (v1 == null || v2 == null) throw new IllegalArgumentException("版本不存在");
        // 简化 diff：返回两个版本的 graphJson（前端计算节点级 diff）
        return java.util.Map.of(
                "v1", java.util.Map.of("versionId", v1Id, "graphJson", v1.getGraphJson()),
                "v2", java.util.Map.of("versionId", v2Id, "graphJson", v2.getGraphJson())
        );
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
