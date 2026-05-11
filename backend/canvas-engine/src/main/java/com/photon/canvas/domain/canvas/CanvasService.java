package com.photon.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.photon.canvas.common.PageResult;
import com.photon.canvas.dto.*;
import com.photon.canvas.engine.dag.DagGraph;
import com.photon.canvas.engine.dag.DagParser;
import com.photon.canvas.infra.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CanvasService {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;
    private final TriggerRouteService triggerRouteService;

    @Transactional
    public Canvas create(CanvasCreateReq req) {
        Canvas canvas = new Canvas();
        canvas.setName(req.getName());
        canvas.setDescription(req.getDescription());
        canvas.setStatus(0);
        canvas.setCreatedBy(req.getCreatedBy());
        canvasMapper.insert(canvas);

        // 若带初始 JSON，创建第一个草稿版本
        if (req.getGraphJson() != null && !req.getGraphJson().isBlank()) {
            CanvasVersion v = new CanvasVersion();
            v.setCanvasId(canvas.getId());
            v.setVersion(1);
            v.setGraphJson(req.getGraphJson());
            v.setStatus(0);
            v.setCreatedBy(req.getCreatedBy());
            canvasVersionMapper.insert(v);
        }
        return canvas;
    }

    public CanvasDetailDTO getById(Long id) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) return null;

        CanvasVersion draft = latestDraft(id);

        CanvasDetailDTO dto = new CanvasDetailDTO();
        dto.setCanvas(canvas);
        dto.setGraphJson(draft != null ? draft.getGraphJson() : "{\"nodes\":[]}");
        dto.setDraftVersionId(draft != null ? draft.getId() : null);
        return dto;
    }

    @Transactional
    public void updateDraft(Long id, CanvasUpdateReq req) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        canvas.setName(req.getName());
        canvas.setDescription(req.getDescription());
        canvasMapper.updateById(canvas);

        if (req.getGraphJson() == null) return;

        CanvasVersion existing = latestDraft(id);
        if (existing != null) {
            existing.setGraphJson(req.getGraphJson());
            canvasVersionMapper.updateById(existing);
        } else {
            int nextVer = nextVersionNumber(id);
            CanvasVersion v = new CanvasVersion();
            v.setCanvasId(id);
            v.setVersion(nextVer);
            v.setGraphJson(req.getGraphJson());
            v.setStatus(0);
            v.setCreatedBy(req.getUpdatedBy());
            canvasVersionMapper.insert(v);
        }
    }

    public PageResult<Canvas> list(CanvasListQuery q) {
        LambdaQueryWrapper<Canvas> wrapper = new LambdaQueryWrapper<Canvas>()
                .eq(q.getStatus() != null, Canvas::getStatus, q.getStatus())
                .like(q.getName() != null && !q.getName().isBlank(), Canvas::getName, q.getName())
                .orderByDesc(Canvas::getCreatedAt);

        IPage<Canvas> page = canvasMapper.selectPage(new Page<>(q.getPage(), q.getSize()), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional
    public CanvasVersion publish(Long id, String operator) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        CanvasVersion draft = latestDraft(id);
        if (draft == null) throw new IllegalStateException("没有可发布的草稿");

        // DAG 校验（Kahn 算法环检测 + 触发器节点检查）
        DagGraph graph = dagParser.parse(draft.getGraphJson());
        if (graph.entryNodes().isEmpty()) {
            throw new IllegalStateException("画布缺少有效触发器节点");
        }

        // 生成发布版本快照
        CanvasVersion published = new CanvasVersion();
        published.setCanvasId(id);
        published.setVersion(nextVersionNumber(id));
        published.setGraphJson(draft.getGraphJson());
        published.setStatus(1);
        published.setCreatedBy(operator);
        canvasVersionMapper.insert(published);

        // 同步删除草稿（发布后草稿无意义，可选）
        // canvasVersionMapper.deleteById(draft.getId());

        canvas.setStatus(1);
        canvas.setPublishedVersionId(published.getId());
        canvasMapper.updateById(canvas);

        // 注册触发路由到 Redis
        registerTriggerRoutes(canvas.getId(), graph);

        return published;
    }

    @Transactional
    public void offline(Long id, String operator) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        canvas.setStatus(2);
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);

        // 清理 Redis 触发路由
        clearTriggerRoutes(id);
    }

    public List<CanvasVersion> getVersions(Long canvasId) {
        return canvasVersionMapper.selectList(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersion::getVersion)
        );
    }

    public CanvasVersion getVersion(Long canvasId, Long versionId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getId, versionId)
        );
    }

    // ── private helpers ──────────────────────────────────────

    private CanvasVersion latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getStatus, 0)
                        .orderByDesc(CanvasVersion::getVersion)
                        .last("LIMIT 1")
        );
    }

    private int nextVersionNumber(Long canvasId) {
        CanvasVersion max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersion::getVersion)
                        .last("LIMIT 1")
        );
        return max != null ? max.getVersion() + 1 : 1;
    }

    @SuppressWarnings("unchecked")
    private void registerTriggerRoutes(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.entryNodes()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || node.getConfig() == null) continue;
            Map<String, Object> cfg = node.getConfig();
            switch (node.getType()) {
                case "MQ_TRIGGER"      -> { String k = (String) cfg.get("topicKey");   if (k != null) triggerRouteService.registerMq(canvasId, k); }
                case "BEHAVIOR_IN_APP" -> { String k = (String) cfg.get("eventCode");  if (k != null) triggerRouteService.registerBehavior(canvasId, k); }
                case "TAGGER_REALTIME" -> { String k = (String) cfg.get("tagCodeKey"); if (k != null) triggerRouteService.registerTagger(canvasId, k); }
                default -> {} // DIRECT_CALL / SCHEDULED_TRIGGER 无需注册
            }
        }
    }

    private void clearTriggerRoutes(Long canvasId) {
        // 找到当前发布版本的触发器节点并清理
        Canvas canvas = canvasMapper.selectById(canvasId);
        if (canvas == null || canvas.getPublishedVersionId() == null) return;
        CanvasVersion v = canvasVersionMapper.selectById(canvas.getPublishedVersionId());
        if (v == null) return;
        DagGraph graph = dagParser.parse(v.getGraphJson());
        for (String nodeId : graph.entryNodes()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null || node.getConfig() == null) continue;
            Map<String, Object> cfg = node.getConfig();
            switch (node.getType()) {
                case "MQ_TRIGGER"      -> { String k = (String) cfg.get("topicKey");   if (k != null) triggerRouteService.removeMq(canvasId, k); }
                case "BEHAVIOR_IN_APP" -> { String k = (String) cfg.get("eventCode");  if (k != null) triggerRouteService.removeBehavior(canvasId, k); }
                case "TAGGER_REALTIME" -> { String k = (String) cfg.get("tagCodeKey"); if (k != null) triggerRouteService.removeTagger(canvasId, k); }
                default -> {}
            }
        }
    }
}
