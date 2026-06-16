package org.chovy.canvas.execution.domain;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeGate {

    private final AtomicBoolean executing = new AtomicBoolean(false);
    private final AtomicBoolean repeatPending = new AtomicBoolean(false);

    public boolean tryEnter() {
        boolean entered = executing.compareAndSet(false, true);
        if (!entered) {
            markRepeat();
        }
        return entered;
    }

    public void markRepeat() {
        repeatPending.set(true);
    }

    public boolean release() {
        executing.set(false);
        return repeatPending.getAndSet(false);
    }
}
