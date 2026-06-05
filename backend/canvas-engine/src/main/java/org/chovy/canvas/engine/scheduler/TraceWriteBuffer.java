package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.chovy.canvas.infrastructure.doris.DorisStreamLoader;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行轨迹异步批量写入缓冲区（设计文档 12.10节）。
 *
 * 主执行链路调用 offer() 非阻塞（<1μs），不等待 DB 写入。
 * 后台线程每 500ms 或积满 200 条时批量 INSERT，降低主链路延迟 5~20ms。
 *
 * 背压保护：队列上限 50000 条，超出时丢弃并告警。
 */
@Slf4j
@Component
public class TraceWriteBuffer {

    /** 单批轨迹刷盘条数。 */
    private static final int BATCH_SIZE   = 200;
    /** 内存缓冲区最大容量。 */
    private static final int MAX_CAPACITY = 50_000;
    /** 单次定时 flush 最多处理的批次数。 */
    private static final int MAX_BATCHES_PER_FLUSH = 20;

    /** 待批量写入的执行轨迹内存队列。 */
    private final ConcurrentLinkedQueue<CanvasExecutionTraceDO> buffer =
            new ConcurrentLinkedQueue<>();
    /** 执行轨迹 Mapper。 */
    private final CanvasExecutionTraceMapper traceMapper;
    /** Doris Stream Load 写入器；迁移期与 MySQL 双写，禁用时为空操作。 */
    private final DorisStreamLoader dorisStreamLoader;
    /** 当前待刷盘轨迹数量。 */
    private final AtomicInteger pending = new AtomicInteger(0);

    public TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper) {
        this(traceMapper, null);
    }

    @Autowired
    public TraceWriteBuffer(CanvasExecutionTraceMapper traceMapper, DorisStreamLoader dorisStreamLoader) {
        this.traceMapper = traceMapper;
        this.dorisStreamLoader = dorisStreamLoader;
    }

    /** 非阻塞入队（主执行链路调用，不等待） */
    public void offer(CanvasExecutionTraceDO trace) {
        int current = pending.incrementAndGet();
        if (current > MAX_CAPACITY) {
            // 背压保护：宁可丢 trace，也不能让主执行链路被轨迹写入拖垮。
            pending.decrementAndGet();
            log.warn("[TRACE_BUFFER] 缓冲区已满（{}），丢弃轨迹 executionId={}",
                    MAX_CAPACITY, trace.getExecutionId());
            return;
        }
        // 只入内存队列，真正 DB 写入由定时 flush 批量完成。
        buffer.offer(trace);
    }

    /** 每 500ms 批量刷盘，每次最多 200 条 */
    @Scheduled(fixedDelay = 500)
    public void flush() {
        if (pending.get() <= 0) return;

        for (int i = 0; i < MAX_BATCHES_PER_FLUSH; i++) {
            List<CanvasExecutionTraceDO> batch = drainBatch();
            if (batch.isEmpty()) return;
            // 单次调度最多刷 MAX_BATCHES_PER_FLUSH 批，避免 flush 长时间占用调度线程。
            writeBatch(batch);
        }
    }

    /** 应用关闭前将缓冲区剩余轨迹全部刷盘，防止丢失 */
    @PreDestroy
    public void shutdownFlush() {
        int remaining = pending.get();
        if (remaining == 0) return;
        log.info("[TRACE_BUFFER] 关闭前刷盘，剩余 {} 条", remaining);
        // 循环直到清空，不受 BATCH_SIZE 限制
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null) {
            batch.add(item);
            pending.decrementAndGet();
            if (batch.size() >= BATCH_SIZE) {
                writeBatch(batch);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) writeBatch(batch);
        log.info("[TRACE_BUFFER] 关闭刷盘完成");
    }

    /**
     * 执行 drain Batch 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 查询、转换或计算得到的结果集合
     */
    private List<CanvasExecutionTraceDO> drainBatch() {
        List<CanvasExecutionTraceDO> batch = new ArrayList<>(BATCH_SIZE);
        CanvasExecutionTraceDO item;
        while ((item = buffer.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(item);
        }
        if (!batch.isEmpty()) {
            // pending 与队列 poll 成功数量保持一致，作为 backlog gauge 的数据源。
            pending.addAndGet(-batch.size());
        }
        return batch;
    }

    /**
     * 写入或记录 write Batch 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param batch batch 方法执行所需的业务参数
     */
    private void writeBatch(List<CanvasExecutionTraceDO> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        try {
            traceMapper.insertBatch(batch);
            log.debug("[TRACE_BUFFER] MySQL 批量写入 {} 条", batch.size());
        } catch (Exception e) {
            // trace 是旁路审计数据，写失败只记录日志，不反向影响节点执行结果。
            log.error("[TRACE_BUFFER] MySQL 批量写入失败: {}", e.getMessage());
        }

        try {
            if (dorisStreamLoader != null) {
                dorisStreamLoader.load(batch);
            }
        } catch (Exception e) {
            // Doris 是迁移期 OLAP 旁路，失败不能反向影响主链路或 MySQL fallback。
            log.warn("[TRACE_BUFFER] Doris Stream Load 失败: {}", e.getMessage());
        }
    }

    /**
     * 执行 pending Count 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    public int pendingCount() { return pending.get(); }
}
