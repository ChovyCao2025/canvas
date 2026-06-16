package org.chovy.canvas.canvas.application;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 封装AfterCommitExecutor相关的业务逻辑。
 */
final class AfterCommitExecutor {

    /**
     * 创建当前对象实例。
     */
    private AfterCommitExecutor() {
    }

    /**
     * 处理runAfterCommitOrNow。
     */
    static void runAfterCommitOrNow(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            /**
             * 在事务提交后执行委托任务。
             */
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
