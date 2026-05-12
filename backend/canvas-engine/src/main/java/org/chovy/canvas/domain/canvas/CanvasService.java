package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dto.*;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasService {

    private final CanvasMapper           canvasMapper;
    private final CanvasVersionMapper    canvasVersionMapper;
    private final DagParser              dagParser;
    private final TriggerRouteService    triggerRouteService;
    private final CanvasSchedulerService schedulerService;
    private final CanvasConfigCache      configCache;
    private final GroovyHandler          groovyHandler;  // 用于 Groovy 预编译（12.9节）

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
        // 注册定时调度任务
        schedulerService.registerScheduledTriggers(canvas.getId(), graph);
        // 使旧版本缓存失效（含 L1 Caffeine 广播）
        if (canvas.getPublishedVersionId() != null) {
            configCache.invalidate(canvas.getId(), canvas.getPublishedVersionId());
        }
        // Groovy 脚本预编译（off-path，避免首次执行时的编译开销，12.9节）
        precompileGroovyNodes(canvas.getId(), graph);

        return published;
    }

    /**
     * 画布下线。
     * 分两步执行：
     *   1. @Transactional DB 操作（状态置 2，清空 publishedVersionId）
     *   2. 事务提交后再做外部调用（Redis 路由清理、Scheduler 取消、缓存失效）
     *
     * 不在事务内调用外部系统的原因：Redis/Scheduler 失败无法回滚 DB，
     * 两者耦合会导致状态不一致。事务外失败最多是"路由残留"，比"状态回滚"危害小。
     */
    public void offline(Long id, String operator) {
        Long publishedVersionId = offlineDb(id);   // Step 1: 事务内 DB 操作
        if (publishedVersionId != null) {
            CanvasVersion v = canvasVersionMapper.selectById(publishedVersionId);
            if (v != null) {
                DagGraph graph = dagParser.parse(v.getGraphJson());
                clearTriggerRoutesFromGraph(id, graph);          // Step 2: 事务外 Redis
                schedulerService.cancelScheduledTriggers(id, graph); // 事务外 Scheduler
                configCache.invalidate(id, publishedVersionId);      // 事务外缓存
            }
        }
    }

    @Transactional
    Long offlineDb(Long id) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvas.setStatus(2);
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);
        return publishedVersionId;
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

    /** 直接从已有 DagGraph 清理路由（offline 使用，避免再次查 DB） */
    private void clearTriggerRoutesFromGraph(Long canvasId, DagGraph graph) {
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

    private void clearTriggerRoutes(Long canvasId) {
        Canvas canvas = canvasMapper.selectById(canvasId);
        if (canvas == null || canvas.getPublishedVersionId() == null) return;
        CanvasVersion v = canvasVersionMapper.selectById(canvas.getPublishedVersionId());
        if (v == null) return;
        clearTriggerRoutesFromGraph(canvasId, dagParser.parse(v.getGraphJson()));
    }

    /**
     * 发布时预编译所有 GROOVY 节点脚本（设计文档 12.9节）。
     * 异步执行，不阻塞发布流程。
     */
    @SuppressWarnings("unchecked")
    private void precompileGroovyNodes(Long canvasId, DagGraph graph) {
        Thread.ofVirtual().start(() -> {
            graph.getNodeMap().forEach((nodeId, node) -> {
                if (!"GROOVY".equals(node.getType())) return;
                Map<String, Object> config = node.getConfig();
                if (config == null) return;
                String code = (String) config.get("code");
                if (code != null && !code.isBlank()) {
                    groovyHandler.precompileScript(canvasId, nodeId, code);
                }
            });
            log.info("[PUBLISH] Groovy 预编译完成 canvasId={}", canvasId);
        });
    }
}
