package org.chovy.canvas.execution.domain;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定义 NodeGate 的执行上下文数据结构或业务契约。
 */
public final class NodeGate {

    private final AtomicBoolean executing = new AtomicBoolean(false);
    private final AtomicBoolean repeatPending = new AtomicBoolean(false);

    /**
     * 执行 tryEnter 对应的业务处理。
     */
    public boolean tryEnter() {
        boolean entered = executing.compareAndSet(false, true);
        if (!entered) {
            markRepeat();
        }
        return entered;
    }

    /**
     * 执行 markRepeat 对应的业务处理。
     */
    public void markRepeat() {
        repeatPending.set(true);
    }

    /**
     * 执行 release 对应的业务处理。
     * @return 处理后的结果
     */
    public boolean release() {
        executing.set(false);
        return repeatPending.getAndSet(false);
    }
}
