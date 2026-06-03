package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * 仅负责"数据库状态变更"的事务服务。
 *
 * <p>目的：把 Redis / Scheduler / Cache 这类外部副作用放在事务外执行，
 * 避免出现"外部调用失败导致 DB 事务回滚"或"DB 回滚但 Redis 已变更"的不一致问题。
 */
@Slf4j
@Service
public class CanvasTransactionService {

    /** 画布 Mapper，仅在事务内修改画布主表状态。 */
    private final CanvasMapper canvasMapper;
    /** 画布版本 Mapper。 */
    private final CanvasVersionMapper canvasVersionMapper;
    /** 画布状态机，集中约束生命周期转换。 */
    private final CanvasStateTransitionPolicy stateTransitionPolicy;

    CanvasTransactionService(CanvasMapper canvasMapper, CanvasVersionMapper canvasVersionMapper) {
        this(canvasMapper, canvasVersionMapper, new CanvasStateTransitionPolicy());
    }

    @Autowired
    public CanvasTransactionService(CanvasMapper canvasMapper,
                                    CanvasVersionMapper canvasVersionMapper,
                                    CanvasStateTransitionPolicy stateTransitionPolicy) {
        this.canvasMapper = canvasMapper;
        this.canvasVersionMapper = canvasVersionMapper;
        this.stateTransitionPolicy = stateTransitionPolicy;
    }

// ── 发布事务 ──────────────────────────────────────────────────

    /**
     * 发布事务（DB-only）。
     *
     * <p>只负责创建发布版本快照、更新画布状态字段，不涉及任何 Redis/Scheduler/Cache 操作。
     * 调用方（{@link CanvasService#publish}）在事务外完成验证和 DAG 解析后调用此方法，
     * 事务提交后再执行路由注册、调度更新、缓存失效等外部副作用。
     *
     * <p>Fix 1：原 CanvasService.publish 将 clearPublishedExternalState 和 registerTriggerRoutes
     * 混在 @Transactional 内，DB 回滚时 Redis 路由状态已改变，造成不可逆不一致。
     * 现拆分为两阶段，本方法仅做 DB 写入。
     *
     * @return {@link PublishResult} 包含新发布版本和旧 publishedVersionId（供清旧路由使用）
     */
    @Transactional
    public PublishResult publishDb(Long canvasId, String graphJson, String operator) {
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + canvasId);
        stateTransitionPolicy.assertTransition(canvas, CanvasStatusEnum.PUBLISHED);

        // 先保留旧发布版本 ID，事务提交后由调用方据此清理旧 Redis 路由和调度。
        Long oldPublishedVersionId = canvas.getPublishedVersionId();

        CanvasVersionDO published = new CanvasVersionDO();
        published.setTenantId(canvas.getTenantId());
        published.setCanvasId(canvasId);
        published.setVersion(nextVersionNumber(canvasId));
        published.setGraphJson(graphJson);
        published.setStatus(VersionStatus.PUBLISHED.getCode());
        published.setCreatedBy(operator);
        canvasVersionMapper.insert(published);

        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(published.getId());
        // 主表只指向刚创建的不可变快照，外部副作用不进入本事务。
        canvasMapper.updateById(canvas);

        return new PublishResult(published, oldPublishedVersionId);
    }

    /** 发布操作的结果载体，供事务外清理旧路由和注册新路由使用。 */
    public record PublishResult(
            /** 本次发布创建的版本快照。 */
            CanvasVersionDO publishedVersion,
            /** 发布前旧版本 ID，用于事务外清理旧路由和调度。 */
            Long oldPublishedVersionId
    ) {}

    /**
     * 执行 offline Db 对应的业务逻辑。
     *
     * <p>该方法在事务边界内执行，确保相关数据库写入保持一致。
     *
     * @param id id 对应的业务主键或标识
     * @return 计算得到的数值结果
     */
// ── 下线事务 ──────────────────────────────────────────────────

    /**
     * 下线事务（DB-only）。
     * 返回下线前的 publishedVersionId，供事务外清理路由/缓存。
     */
    @Transactional
    Long offlineDb(Long id) {
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        stateTransitionPolicy.assertTransition(canvas, CanvasStatusEnum.OFFLINE);
        // publishedVersionId 在清空前返回，供事务外清理该版本的路由、调度和缓存。
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvas.setStatus(CanvasStatusEnum.OFFLINE.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);
        return publishedVersionId;
    }

    /**
     * 执行 kill Db 对应的业务逻辑。
     *
     * <p>该方法在事务边界内执行，确保相关数据库写入保持一致。
     *
     * @param id id 对应的业务主键或标识
     * @return 计算得到的数值结果
     */
// ── Kill 事务 ─────────────────────────────────────────────────

    /**
     * Kill 事务（DB-only）。
     * 返回 kill 前的 publishedVersionId，供事务外清理路由/缓存。
     */
    @Transactional
    public Long killDb(Long id) {
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        stateTransitionPolicy.assertTransition(canvas, CanvasStatusEnum.KILLED);
        // Kill 与下线同样先切 DB 状态，再由事务外广播和清理运行时状态。
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);
        return publishedVersionId;
    }

    /**
     * 执行 archive Db 对应的业务逻辑。
     *
     * <p>该方法在事务边界内执行，确保相关数据库写入保持一致。
     *
     * @param id id 对应的业务主键或标识
     */
// ── 归档事务 ──────────────────────────────────────────────────

    /** 归档事务（DB-only）：仅修改状态为 ARCHIVED，不清 publishedVersionId。 */
    @Transactional
    void archiveDb(Long id) {
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);
        stateTransitionPolicy.assertTransition(canvas, CanvasStatusEnum.ARCHIVED);
        // 归档保留 publishedVersionId 审计线索，外部状态是否清理由调用方按归档前状态判断。
        canvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
        canvasMapper.updateById(canvas);
    }

    /**
     * 执行 next Version Number 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 计算得到的数值结果
     */
// ── 私有辅助 ──────────────────────────────────────────────────

    /** 查询当前最大版本号并生成下一次发布快照版本号。 */
    private int nextVersionNumber(Long canvasId) {
        CanvasVersionDO max = canvasVersionMapper.selectOne(
                new LambdaQueryWrapper<CanvasVersionDO>()
                        .eq(CanvasVersionDO::getCanvasId, canvasId)
                        .orderByDesc(CanvasVersionDO::getVersion)
                        .last("LIMIT 1"));
        return max != null ? max.getVersion() + 1 : 1;
    }
}
