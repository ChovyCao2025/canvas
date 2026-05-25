package org.chovy.canvas.domain.canvas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.engine.dag.DagGraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;

/**
 * 仅负责“数据库状态变更”的事务服务。
 *
 * <p>目的：把 Redis / Scheduler / Cache 这类外部副作用放在事务外执行，
 * 避免出现“外部调用失败导致 DB 事务回滚”的级联问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasTransactionService {
    /** 画布主表访问层，仅做事务内数据更新。 */
    private final CanvasMapper  canvasMapper;

    /**
     * 下线事务。
     *
     * <p>步骤：
     * 1) 校验画布存在；
     * 2) 记录当前 publishedVersionId（供事务外清缓存/清路由）；
     * 3) 置状态为 OFFLINE，并清空 publishedVersionId。
     *
     * <p>返回值：
     * - 返回下线前的发布版本 ID，供事务外清理路由/缓存使用；
     * - 可能为 null（画布从未发布过）。
     */
    @Transactional
    Long offlineDb(Long id) {
        // 1) 读取当前画布快照（事务内）
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        // 2) 保留下线前发布版本，供事务外清缓存/清路由
        Long publishedVersionId = canvas.getPublishedVersionId();

        // 3) 更新主表状态
        canvas.setStatus(CanvasStatusEnum.OFFLINE.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);

        // 4) 返回给上层做后置清理
        return publishedVersionId;
    }

    /**
     * Kill 事务。
     *
     * <p>步骤：
     * 1) 校验画布存在；
     * 2) 记录当前 publishedVersionId（供事务外清路由/缓存）；
     * 3) 置状态为 KILLED，清空 publishedVersionId。
     *
     * <p>为何与 offlineDb 分开：KILLED 是不可恢复的紧急停止，语义与 OFFLINE 不同；
     * 此处仅做 DB 变更，外部副作用（路由/调度/缓存清理）由 {@link CanvasService#applyKillExternalCleanup} 完成。
     */
    @Transactional
    public Long killDb(Long id) {
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("Canvas not found: " + id);
        Long publishedVersionId = canvas.getPublishedVersionId();
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        canvas.setPublishedVersionId(null);
        canvasMapper.updateById(canvas);
        return publishedVersionId;
    }

    @Transactional
    void archiveDb(Long id) {
        // 1) 读取并校验画布存在性
        CanvasDO canvas = canvasMapper.selectById(id);
        if (canvas == null) throw new IllegalArgumentException("画布不存在: " + id);

        // 2) 仅变更状态，不触碰发布版本字段
        canvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
        canvasMapper.updateById(canvas);
    }
}
