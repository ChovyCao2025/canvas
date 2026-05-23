package org.chovy.canvas.domain.canvas;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.domain.constant.NodeType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.constant.VersionStatus;
import org.chovy.canvas.dto.*;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infra.cache.CanvasConfigCache;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 画布主服务：负责画布生命周期与版本管理。
 *
 * <p>核心职责：
 * <ul>
 *   <li>草稿创建/更新与分页查询</li>
 *   <li>发布/下线/归档时的 DB 状态切换与外部副作用协调</li>
 *   <li>版本回退、历史版本查询</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasService {

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final DagParser dagParser;
    private final TriggerRouteService triggerRouteService;
    private final CanvasSchedulerService schedulerService;
    private final CanvasConfigCache configCache;
    private final CanvasExecutionService canvasExecutionService;
    private final GroovyHandler groovyHandler;
    private final org.chovy.canvas.engine.handlers.MqTriggerHandler mqTriggerHandler;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;
    private final CanvasTransactionService canvasTransactionService;

    /**
     * 创建画布
     *
     * @param req 创建请求信息
     * @return 新建的画布对象
     */
    @Transactional
    public Canvas create(CanvasCreateReq req) {
        Canvas canvas = new Canvas();
        canvas.setName(req.getName());
        canvas.setDescription(req.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvasMapper.insert(canvas);

        // 若带初始 JSON，创建第一个草稿版本
        if (StrUtil.isNotBlank(req.getGraphJson())) {
            CanvasVersion v = new CanvasVersion();
            v.setCanvasId(canvas.getId());
            v.setVersion(1);
            v.setGraphJson(req.getGraphJson());
            v.setStatus(VersionStatus.DRAFT.getCode());
            v.setCreatedBy(req.getCreatedBy());
            canvasVersionMapper.insert(v);
        }
        return canvas;
    }

    /**
     * 根据 ID 获取画布详情
     *
     * @param id 画布 ID
     * @return 画布详情 DTO
     */
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

    /**
     * 更新画布草稿
     *
     * @param id  画布 ID
     * @param req 画布更新请求
     */
    @Transactional
    public void updateDraft(Long id, CanvasUpdateReq req) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        canvas.setName(req.getName());
        canvas.setDescription(req.getDescription());
        if (req.getTriggerType() != null) canvas.setTriggerType(req.getTriggerType());
        canvas.setCronExpression(req.getCronExpression());
        canvas.setValidStart(req.getValidStart());
        canvas.setValidEnd(req.getValidEnd());
        canvas.setMaxTotalExecutions(req.getMaxTotalExecutions());
        canvas.setPerUserDailyLimit(req.getPerUserDailyLimit());
        canvas.setPerUserTotalLimit(req.getPerUserTotalLimit());
        canvas.setCooldownSeconds(req.getCooldownSeconds());
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
            v.setStatus(VersionStatus.DRAFT.getCode());
            v.setCreatedBy(req.getUpdatedBy());
            canvasVersionMapper.insert(v);
        }
    }

    /**
     * 分页查询画布列表
     *
     * @param q 查询条件
     * @return 分页结果
     */
    public PageResult<Canvas> list(CanvasListQuery q) {
        LambdaQueryWrapper<Canvas> wrapper = new LambdaQueryWrapper<Canvas>()
                .eq(q.getStatus() != null, Canvas::getStatus, q.getStatus())
                .ne(q.getStatus() == null, Canvas::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
                .like(q.getName() != null && !q.getName().isBlank(), Canvas::getName, q.getName())
                .orderByDesc(Canvas::getCreatedAt);

        IPage<Canvas> page = canvasMapper.selectPage(new Page<>(q.getPage(), q.getSize()), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /**
     * 发布画布
     *
     * @param id       画布 ID
     * @param operator 操作人
     * @return 发布版本信息
     */
    @Transactional
    public CanvasVersion publish(Long id, String operator) {

        // 并发发布保护（设计文档 6.2节）：同一画布同时只允许一个发布操作
        String lockKey = "canvas:publish:lock:" + id;
        String lockVal = java.util.UUID.randomUUID().toString();
        boolean acquired = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(lockKey, lockVal,
                        java.time.Duration.ofSeconds(30)));
        if (!acquired) throw new IllegalStateException("CANVAS_011: 画布正在发布中，请稍后重试");

        try {
            Canvas canvas = canvasMapper.selectById(id);
            if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

            CanvasVersion draft = latestDraft(id);
            if (draft == null) throw new IllegalStateException("没有可发布的草稿");

            // DAG 校验（Kahn 算法环检测 + 触发器节点检查）
            DagGraph graph = dagParser.parse(draft.getGraphJson());
            if (graph.entryNodes().isEmpty()) {
                throw new IllegalStateException("画布缺少有效触发器节点");
            }

            // SUB_FLOW_REF 依赖校验（设计文档 6.2节补充）
            validateSubFlowDependencies(id, graph);

            // 生成发布版本快照
            CanvasVersion published = new CanvasVersion();
            published.setCanvasId(id);
            published.setVersion(nextVersionNumber(id));
            published.setGraphJson(draft.getGraphJson());
            published.setStatus(VersionStatus.PUBLISHED.getCode());
            published.setCreatedBy(operator);
            canvasVersionMapper.insert(published);

            canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
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
            // 发布后驱逐 Canvas 实体缓存，下次 trigger() 读到最新 publishedVersionId/status
            canvasExecutionService.invalidateCanvas(id);
            // Groovy 脚本预编译（off-path，12.9节）
            precompileGroovyNodes(canvas.getId(), graph);

            return published;
        } finally {
            redis.delete(lockKey); // 发布结束无论成功失败都释放锁
        }
    }

    /**
     * SUB_FLOW_REF 依赖校验（设计文档 6.2节）：
     * 检查画布中所有 SUB_FLOW_REF 节点引用的子流程是否已发布。
     * 若子流程未发布，运行时会失败，应在发布时就拦截。
     */
    private void validateSubFlowDependencies(Long canvasId, DagGraph graph) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        graph.getNodeMap().forEach((nodeId, node) -> {
            if (!NodeType.SUB_FLOW_REF.equals(node.getType())) return;
            Map<String, Object> cfg = node.getConfig();
            if (cfg == null) return;
            Object subFlowIdObj = cfg.get("subFlowId");
            if (subFlowIdObj == null) return;
            Long subFlowId = Long.parseLong(String.valueOf(subFlowIdObj));
            Canvas subFlow = canvasMapper.selectById(subFlowId);
            if (subFlow == null) {
                errors.add("节点「" + node.getName() + "」引用的子流程 ID=" + subFlowId + " 不存在");
            } else if (!Objects.equals(subFlow.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
                errors.add("节点「" + node.getName() + "」引用的子流程「" + subFlow.getName() + "」未发布（当前状态: " + subFlow.getStatus() + "）");
            } else if (subFlowId.equals(canvasId)) {
                errors.add("节点「" + node.getName() + "」引用了自身，会产生循环调用");
            }
        });
        if (!errors.isEmpty()) {
            throw new IllegalStateException("发布校验失败：\n" + String.join("\n", errors));
        }
    }

    /**
     * 画布下线。
     * 分两步执行：
     * 1. @Transactional DB 操作（状态置 2，清空 publishedVersionId）
     * 2. 事务提交后再做外部调用（Redis 路由清理、Scheduler 取消、缓存失效）
     * 不在事务内调用外部系统的原因：Redis/Scheduler 失败无法回滚 DB，
     * 两者耦合会导致状态不一致。事务外失败最多是"路由残留"，比"状态回滚"危害小。
     */
    public void offline(Long id, String operator) {
        Long publishedVersionId = canvasTransactionService.offlineDb(id);   // Step 1: 事务内 DB 操作
        if (publishedVersionId != null) {
            CanvasVersion v = canvasVersionMapper.selectById(publishedVersionId);
            if (v != null) {
                DagGraph graph = dagParser.parse(v.getGraphJson());
                clearTriggerRoutesFromGraph(id, graph);          // Step 2: 事务外 Redis
                schedulerService.cancelScheduledTriggers(id, graph); // 事务外 Scheduler
                configCache.invalidate(id, publishedVersionId);      // 事务外缓存
                canvasExecutionService.invalidateCanvas(id);          // 驱逐 Canvas 实体缓存
            }
        }
    }

    /**
     * 归档画布：
     * <ol>
     *   <li>事务内只修改 DB 状态为 ARCHIVED</li>
     *   <li>若归档前处于已发布状态，则在事务外清理路由/调度/缓存</li>
     * </ol>
     */
    public void archive(Long id, String operator) {
        Canvas canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        if (CanvasStatusEnum.ARCHIVED.getCode().equals(canvas.getStatus())) {
            throw new IllegalStateException("画布已归档: " + id);
        }
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvasTransactionService.archiveDb(id);
        // If was PUBLISHED, clean up external state exactly like offline() does
        if (CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus())) {
            if (publishedVersionId != null) {
                CanvasVersion v = canvasVersionMapper.selectById(publishedVersionId);
                if (v != null) {
                    DagGraph graph = dagParser.parse(v.getGraphJson());
                    clearTriggerRoutesFromGraph(id, graph);          // Redis 路由清理
                    schedulerService.cancelScheduledTriggers(id, graph); // Scheduler 取消
                    configCache.invalidate(id, publishedVersionId);      // 缓存失效
                    canvasExecutionService.invalidateCanvas(id);          // 驱逐 Canvas 实体缓存
                }
            }
        }
    }

    /**
     * 分页查询画布版本历史（按版本号倒序）。
     */
    public PageResult<CanvasVersion> getVersions(Long canvasId, int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CanvasVersion> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<CanvasVersion> q = new LambdaQueryWrapper<CanvasVersion>()
                .eq(CanvasVersion::getCanvasId, canvasId)
                .orderByDesc(CanvasVersion::getVersion);
        var result = canvasVersionMapper.selectPage(p, q);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /**
     * @deprecated 使用 getVersions(canvasId, page, size)
     */
    @Deprecated
    public List<CanvasVersion> getVersions(Long canvasId) {
        return canvasVersionMapper.selectList(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersion::getVersion)
                        .last("LIMIT 20") // 防止全量返回
        );
    }

    /**
     * 查询单个画布版本详情。
     */
    public CanvasVersion getVersion(Long canvasId, Long versionId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getId, versionId)
        );
    }

    // ── private helpers ──────────────────────────────────────

    /** 查询最新草稿版本。 */
    private CanvasVersion latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .eq(CanvasVersion::getStatus, 0)
                        .orderByDesc(CanvasVersion::getVersion)
                        .last("LIMIT 1")
        );
    }

    /** 计算下一个版本号（max + 1）。 */
    private int nextVersionNumber(Long canvasId) {
        CanvasVersion max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersion>()
                        .eq(CanvasVersion::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersion::getVersion)
                        .last("LIMIT 1")
        );
        return max != null ? max.getVersion() + 1 : 1;
    }

    /**
     * Reverts the current draft to the content of a historical version.
     * Does NOT affect the published version.
     */
    @Transactional
    public void revertToVersion(Long canvasId, Long versionId) {
        CanvasVersion target = canvasVersionMapper.selectById(versionId);
        if (target == null) throw new IllegalArgumentException("版本不存在: " + versionId);
        if (!target.getCanvasId().equals(canvasId)) throw new IllegalArgumentException("版本不属于该画布");

        CanvasVersion draft = latestDraft(canvasId);
        if (draft != null) {
            draft.setGraphJson(target.getGraphJson());
            canvasVersionMapper.updateById(draft);
        } else {
            CanvasVersion newDraft = new CanvasVersion();
            newDraft.setCanvasId(canvasId);
            newDraft.setVersion(nextVersionNumber(canvasId));
            newDraft.setGraphJson(target.getGraphJson());
            newDraft.setStatus(VersionStatus.DRAFT.getCode());
            canvasVersionMapper.insert(newDraft);
        }
    }

    private void registerTriggerRoutes(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) continue;
            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig() != null) cfg.putAll(node.getConfig());
            switch (node.getType()) {
                case NodeType.EVENT_TRIGGER -> {
                    String k = (String) cfg.get("eventCode");
                    if (k != null) triggerRouteService.registerBehavior(canvasId, k);
                }
                case NodeType.MQ_TRIGGER -> {
                    String k = mqTriggerHandler.resolveTopic(cfg);
                    if (!k.isEmpty()) triggerRouteService.registerMq(canvasId, k);
                }
                case NodeType.TAGGER_REALTIME -> {
                    String k = (String) cfg.get("tagCodeKey");
                    if (k != null) triggerRouteService.registerTagger(canvasId, k);
                }
                default -> {
                }
            }
        }
    }

    /**
     * 直接从已有 DagGraph 清理路由（offline 使用，避免再次查 DB）
     */
    private void clearTriggerRoutesFromGraph(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) continue;
            Map<String, Object> cfg = new java.util.HashMap<>();
            if (node.getBizConfig() != null) cfg.putAll(node.getBizConfig());
            if (node.getConfig() != null) cfg.putAll(node.getConfig());
            switch (node.getType()) {
                case NodeType.EVENT_TRIGGER -> {
                    String k = (String) cfg.get("eventCode");
                    if (k != null) triggerRouteService.removeBehavior(canvasId, k);
                }
                case NodeType.MQ_TRIGGER -> {
                    String k = mqTriggerHandler.resolveTopic(cfg);
                    if (!k.isEmpty()) triggerRouteService.removeMq(canvasId, k);
                }
                case NodeType.TAGGER_REALTIME -> {
                    String k = (String) cfg.get("tagCodeKey");
                    if (k != null) triggerRouteService.removeTagger(canvasId, k);
                }
                default -> {
                }
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
    private void precompileGroovyNodes(Long canvasId, DagGraph graph) {
        Thread.ofVirtual().start(() -> {
            graph.getNodeMap().forEach((nodeId, node) -> {
                if (!NodeType.GROOVY.equals(node.getType())) return;
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
