package org.chovy.canvas.domain.canvas;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.enums.NodeType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dto.*;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.script.RedisScript;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.query.CanvasListQuery;

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

    /** 画布 Mapper，用于画布主表的生命周期状态读写。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** DAG 解析器，将 graphJson 转换为可执行图结构。 */
    private final DagParser dagParser;
    /** 触发路由服务，负责 Redis 路由注册、清理和查询。 */
    private final TriggerRouteService triggerRouteService;
    /** 画布调度服务，负责定时触发器注册和取消。 */
    private final CanvasSchedulerService schedulerService;
    /** 画布配置缓存，用于执行链路快速读取发布态配置。 */
    private final CanvasConfigCache configCache;
    /** 画布执行服务，负责触发和恢复 DAG 执行。 */
    private final CanvasExecutionService canvasExecutionService;
    /** 触发预检服务，负责有效期、配额、冷却期和并发保护校验。 */
    private final TriggerPreCheckService preCheckService;
    /** Groovy 节点处理器，用于发布前脚本预编译和校验。 */
    private final GroovyHandler groovyHandler;
    /** MQ 触发节点处理器，用于解析和匹配 MQ 触发入口。 */
    private final org.chovy.canvas.engine.handlers.MqTriggerHandler mqTriggerHandler;
    /** 画布规则图校验器，用于发布前验证条件/人群规则结构。 */
    private final CanvasRuleGraphValidator canvasRuleGraphValidator;
    /** 阻塞式 Redis 模板，用于锁、去重、票据或跨实例通知。 */
    private final org.springframework.data.redis.core.StringRedisTemplate redis;
    /** 画布事务服务，封装只涉及数据库写入的事务边界。 */
    private final CanvasTransactionService canvasTransactionService;
    /** 示例画布配置属性，控制示例数据展示和过滤。 */
    private final CanvasExamplesProperties examplesProperties;

    /**
     * 创建画布
     *
     * @param req 创建请求信息
     * @return 新建的画布对象
     */
    @Transactional(rollbackFor = Exception.class)
    public CanvasDO create(CanvasCreateReq req) {
        CanvasDO canvas = new CanvasDO();
        canvas.setName(req.getName());
        canvas.setDescription(req.getDescription());
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvasMapper.insert(canvas);

        // 若带初始 JSON，创建第一个草稿版本
        if (StrUtil.isNotBlank(req.getGraphJson())) {
            CanvasVersionDO v = new CanvasVersionDO();
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
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) return null;

        CanvasVersionDO draft = latestDraft(id);

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
    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(Long id, CanvasUpdateReq req) {
        CanvasDO canvas = canvasMapper.selectById(id);
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

        CanvasVersionDO existing = latestDraft(id);
        if (existing != null) {
            existing.setGraphJson(req.getGraphJson());
            canvasVersionMapper.updateById(existing);
        } else {
            int nextVer = nextVersionNumber(id);
            CanvasVersionDO v = new CanvasVersionDO();
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
    public PageResult<CanvasDO> list(CanvasListQuery q) {
        LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<CanvasDO>()
                .eq(q.getStatus() != null, CanvasDO::getStatus, q.getStatus())
                .ne(q.getStatus() == null, CanvasDO::getStatus, CanvasStatusEnum.ARCHIVED.getCode())
                .eq(!examplesProperties.isEnabled(), CanvasDO::getIsExample, 0)
                .like(q.getName() != null && !q.getName().isBlank(), CanvasDO::getName, q.getName())
                .orderByDesc(CanvasDO::getCreatedAt);

        IPage<CanvasDO> page = canvasMapper.selectPage(new Page<>(q.getPage(), q.getSize()), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /**
     * 发布画布（两阶段，Fix 1）。
     *
     * <p>原实现将 Redis 路由操作（clearPublishedExternalState / registerTriggerRoutes）
     * 混在 @Transactional 内：若 DB 事务回滚，Redis 路由已变更，导致不可逆不一致。
     *
     * <p>现拆分为两阶段：
     * <ol>
     *   <li>验证阶段（事务外）：读操作 + DAG 解析 + 依赖校验；</li>
     *   <li>{@link CanvasTransactionService#publishDb}（事务内）：仅做 DB 写入；</li>
     *   <li>外部副作用（事务外）：清旧路由 → 注册新路由 → 调度 → 缓存失效。</li>
     * </ol>
     * 发布锁（Redis SETNX，TTL 30s）贯穿全流程，防并发发布。
     */
    public CanvasVersionDO publish(Long id, String operator) {

        // 并发发布保护：同一画布同时只允许一个发布操作
        String lockKey = "canvas:publish:lock:" + id;
        String lockVal = java.util.UUID.randomUUID().toString();
        boolean acquired = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(lockKey, lockVal,
                        java.time.Duration.ofSeconds(30)));
        if (!acquired) throw new IllegalStateException("CANVAS_011: 画布正在发布中，请稍后重试");

        try {
            // 阶段1：验证（事务外，全部读操作）
            CanvasDO canvas = canvasMapper.selectById(id);
            if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

            CanvasVersionDO draft = latestDraft(id);
            if (draft == null) throw new IllegalStateException("没有可发布的草稿");

            DagGraph graph = dagParser.parse(draft.getGraphJson());
            if (graph.entryNodes().isEmpty()) {
                throw new IllegalStateException("画布缺少有效触发器节点");
            }
            // 子流程依赖必须在发布事务前完成校验，避免 DB 已切发布态后运行时才发现断链。
            validateSubFlowDependencies(id, graph);
            canvasRuleGraphValidator.validateOrThrow(graph);

            // 阶段2：DB 事务（只写 DB，不碰 Redis/Scheduler/Cache）
            CanvasTransactionService.PublishResult result =
                    canvasTransactionService.publishDb(id, draft.getGraphJson(), operator);

            // 阶段3：事务外副作用（任一步骤失败不回滚 DB，路由/缓存最终通过 TTL 或再次发布自愈）
            // 使用 publishDb 返回的旧版本 ID 精准清理旧路由，避免误删本次刚注册的新发布路由。
            // 3a. 清理旧版本路由和调度
            clearPreviousPublishedState(id, result.oldPublishedVersionId());
            // 3b. 注册新版本路由和调度
            registerTriggerRoutes(id, graph);
            schedulerService.registerScheduledTriggers(id, graph);
            // 3c. 缓存失效
            canvasExecutionService.invalidateCanvas(id);
            // 3d. Groovy 脚本预编译（off-path，12.9节）
            precompileGroovyNodes(id, graph);

            return result.publishedVersion();
        } finally {
            // 原子释放发布锁（Lua check-then-del，防止锁超时后误删其他实例的锁）
            redis.execute(PUBLISH_LOCK_RELEASE_SCRIPT, List.of(lockKey), lockVal);
        }
    }

    /** 释放画布发布锁的 Lua 脚本，保证只释放当前发布流程持有的锁。 */
    private static final RedisScript<Long> PUBLISH_LOCK_RELEASE_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    /**
     * 清理旧发布版本的触发路由、调度任务和配置缓存（阶段3a，事务外）。
     * 取代原 clearPublishedExternalState，语义更明确。
     */
    private void clearPreviousPublishedState(Long canvasId, Long oldPublishedVersionId) {
        if (oldPublishedVersionId == null) return;
        CanvasVersionDO oldV = canvasVersionMapper.selectById(oldPublishedVersionId);
        if (oldV == null) return;
        // 旧发布快照是清理外部状态的唯一依据，不能依赖画布主表当前 publishedVersionId。
        DagGraph oldGraph = dagParser.parse(oldV.getGraphJson());
        clearTriggerRoutesFromGraph(canvasId, oldGraph);
        schedulerService.cancelScheduledTriggers(canvasId, oldGraph);
        configCache.invalidate(canvasId, oldPublishedVersionId);
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
            CanvasDO subFlow = canvasMapper.selectById(subFlowId);
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
            CanvasVersionDO v = canvasVersionMapper.selectById(publishedVersionId);
            if (v != null) {
                // 下线后的 Redis/Scheduler/Cache 清理放在事务外，DB 状态不因外部系统异常回滚。
                DagGraph graph = dagParser.parse(v.getGraphJson());
                clearTriggerRoutesFromGraph(id, graph);          // Step 2: 事务外 Redis
                schedulerService.cancelScheduledTriggers(id, graph); // 事务外 Scheduler
                configCache.invalidate(id, publishedVersionId);      // 事务外缓存
                canvasExecutionService.invalidateCanvas(id);          // 驱逐 CanvasDO 实体缓存
                preCheckService.cleanupCanvasQuotas(id);              // 清理永不过期配额 key
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
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        if (CanvasStatusEnum.ARCHIVED.getCode().equals(canvas.getStatus())) {
            throw new IllegalStateException("画布已归档: " + id);
        }
        Long publishedVersionId = canvas.getPublishedVersionId();
        // 归档事务内只改主表状态，是否需要清外部状态由归档前状态决定。
        canvasTransactionService.archiveDb(id);
        // 若归档前仍是发布态，事务提交后按下线同口径清理外部状态。
        if (CanvasStatusEnum.PUBLISHED.getCode().equals(canvas.getStatus())) {
            if (publishedVersionId != null) {
                CanvasVersionDO v = canvasVersionMapper.selectById(publishedVersionId);
                if (v != null) {
                    DagGraph graph = dagParser.parse(v.getGraphJson());
                    clearTriggerRoutesFromGraph(id, graph);          // Redis 路由清理
                    schedulerService.cancelScheduledTriggers(id, graph); // Scheduler 取消
                    configCache.invalidate(id, publishedVersionId);      // 缓存失效
                    canvasExecutionService.invalidateCanvas(id);          // 驱逐 CanvasDO 实体缓存
                    preCheckService.cleanupCanvasQuotas(id);              // 清理永不过期配额 key
                }
            }
        }
    }

    /**
     * 分页查询画布版本历史（按版本号倒序）。
     */
    public PageResult<CanvasVersionDO> getVersions(Long canvasId, int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CanvasVersionDO> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<CanvasVersionDO> q = new LambdaQueryWrapper<CanvasVersionDO>()
                .eq(CanvasVersionDO::getCanvasId, canvasId)
                .orderByDesc(CanvasVersionDO::getVersion);
        var result = canvasVersionMapper.selectPage(p, q);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /**
     * @deprecated 使用 getVersions(canvasId, page, size)
     */
    @Deprecated
    public List<CanvasVersionDO> getVersions(Long canvasId) {
        return canvasVersionMapper.selectList(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion)
                        .last("LIMIT 20") // 防止全量返回
        );
    }

    /**
     * 查询单个画布版本详情。
     */
    public CanvasVersionDO getVersion(Long canvasId, Long versionId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .eq(CanvasVersionDO::getId, versionId)
        );
    }

    /**
     * Kill 操作的外部状态清理，供 {@link CanvasOpsService#kill} 在事务外调用。
     * 复用与 offline/archive 相同的清理逻辑：路由 → 调度 → 缓存 → 配额。
     */
    public void applyKillExternalCleanup(Long canvasId, Long publishedVersionId) {
        if (publishedVersionId != null) {
            CanvasVersionDO v = canvasVersionMapper.selectById(publishedVersionId);
            if (v != null) {
                // Kill 只接收事务前发布版本 ID，确保清理的是被终止版本的触发入口。
                DagGraph graph = dagParser.parse(v.getGraphJson());
                clearTriggerRoutesFromGraph(canvasId, graph);
                schedulerService.cancelScheduledTriggers(canvasId, graph);
                configCache.invalidate(canvasId, publishedVersionId);
            }
        }
        canvasExecutionService.invalidateCanvas(canvasId);
        preCheckService.cleanupCanvasQuotas(canvasId);
    }

    /**
     * 执行 latest Draft 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
// ── private helpers ──────────────────────────────────────

    /** 查询最新草稿版本。 */
    private CanvasVersionDO latestDraft(Long canvasId) {
        return canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .eq(CanvasVersionDO::getStatus, 0)
                        .orderByDesc(CanvasVersionDO::getVersion)
                        .last("LIMIT 1")
        );
    }

    /** 计算下一个版本号（max + 1）。 */
    private int nextVersionNumber(Long canvasId) {
        CanvasVersionDO max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion)
                        .last("LIMIT 1")
        );
        return max != null ? max.getVersion() + 1 : 1;
    }

    /**
     * Reverts the current draft to the content of a historical version.
     * Does NOT affect the published version.
     */
    @Transactional(rollbackFor = Exception.class)
    public void revertToVersion(Long canvasId, Long versionId) {
        CanvasVersionDO target = canvasVersionMapper.selectById(versionId);
        if (target == null) throw new IllegalArgumentException("版本不存在: " + versionId);
        if (!target.getCanvasId().equals(canvasId)) throw new IllegalArgumentException("版本不属于该画布");

        CanvasVersionDO draft = latestDraft(canvasId);
        if (draft != null) {
            draft.setGraphJson(target.getGraphJson());
            canvasVersionMapper.updateById(draft);
        } else {
            CanvasVersionDO newDraft = new CanvasVersionDO();
            newDraft.setCanvasId(canvasId);
            newDraft.setVersion(nextVersionNumber(canvasId));
            newDraft.setGraphJson(target.getGraphJson());
            newDraft.setStatus(VersionStatus.DRAFT.getCode());
            canvasVersionMapper.insert(newDraft);
        }
    }

    /** 根据发布态 DAG 中的触发节点注册行为事件、MQ 和 Tagger 实时路由。 */
    private void registerTriggerRoutes(Long canvasId, DagGraph graph) {
        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            if (node == null) continue;
            // bizConfig 与 config 合并后再取触发参数，兼容前端不同节点配置落点。
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
            // 按发布快照反向删除路由，避免下线后行为事件、MQ 或 Tagger 继续命中画布。
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

    /** 从当前发布版本反解析 DAG 并清理全部触发路由。 */
    private void clearTriggerRoutes(Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null || canvas.getPublishedVersionId() == null) return;
        CanvasVersionDO v = canvasVersionMapper.selectById(canvas.getPublishedVersionId());
        if (v == null) return;
        clearTriggerRoutesFromGraph(canvasId, dagParser.parse(v.getGraphJson()));
    }

    /**
     * 发布时预编译所有 GROOVY 节点脚本（设计文档 12.9节）。
     * 异步执行，不阻塞发布流程。
     */
    private void precompileGroovyNodes(Long canvasId, DagGraph graph) {
        Thread.ofVirtual().start(() -> {
            // 脚本预编译是发布后的旁路优化，失败不应阻塞发布主链路。
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
