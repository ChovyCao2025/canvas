package org.chovy.canvas.engine.context;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 节点执行门控，分离互斥锁和 repeat 信号两个职责。
 *
 * <ul>
 *   <li>{@link #executing} — 互斥锁：{@code false}=空闲，{@code true}=正在执行。
 *       通过 {@code CAS(false→true)} 抢锁，执行完 {@code set(false)} 释放。</li>
 *   <li>{@link #repeatPending} — repeat 信号：{@code true} 表示有并发协程在本节点
 *       执行期间被拒绝入场，当前执行方完成后需要再跑一次（repeat）。</li>
 * </ul>
 */
public final class NodeGate {

    /** 互斥锁，false=空闲，true=执行中 */
    public final AtomicBoolean executing    = new AtomicBoolean(false);

    /** repeat 信号，true=有并发协程需要 repeat */
    public final AtomicBoolean repeatPending = new AtomicBoolean(false);
}
