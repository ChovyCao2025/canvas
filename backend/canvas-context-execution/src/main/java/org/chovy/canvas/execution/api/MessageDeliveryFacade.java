package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;

/**
 * 定义 MessageDeliveryFacade 的执行上下文数据结构或业务契约。
 */
public interface MessageDeliveryFacade {

    /**
     * 执行 search 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    DeliveryPageView search(DeliverySearchQuery query);

    /**
     * 执行 findById 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    Optional<MessageDeliveryCatalog.Delivery> findById(Long id);

    /**
     * 执行 receipts 对应的业务处理。
     * @param outboxId outboxId 参数
     * @return 处理后的结果
     */
    List<MessageDeliveryCatalog.Receipt> receipts(Long outboxId);

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    ReplayResultView replay(Long id);

    /**
     * 执行 reconcile 对应的业务处理。
     * @return 处理后的结果
     */
    ReconcileResultView reconcile();

    /**
     * 定义 DeliverySearchQuery 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param channel channel 对应的数据字段
     * @param provider provider 对应的数据字段
     * @param status status 对应的数据字段
     * @param providerMessageId providerMessageId 对应的数据字段
     * @param page page 对应的数据字段
     * @param size size 对应的数据字段
     */
    record DeliverySearchQuery(
            Long tenantId,
            Long canvasId,
            String executionId,
            String userId,
            String channel,
            String provider,
            String status,
            String providerMessageId,
            int page,
            int size) {
        public DeliverySearchQuery {
            page = Math.max(1, page);
            size = Math.max(1, Math.min(size, 100));
        }
    }

    /**
     * 定义 DeliveryPageView 的执行上下文数据结构或业务契约。
     * @param total total 对应的数据字段
     * @param list list 对应的数据字段
     */
    record DeliveryPageView(long total, List<MessageDeliveryCatalog.Delivery> list) {
        public DeliveryPageView {
            list = List.copyOf(list == null ? List.of() : list);
        }
    }

    /**
     * 定义 ReplayResultView 的执行上下文数据结构或业务契约。
     * @param outboxId outboxId 对应的数据字段
     * @param status status 对应的数据字段
     * @param replayed replayed 对应的数据字段
     */
    record ReplayResultView(Long outboxId, String status, boolean replayed) {
    }

    /**
     * 定义 ReconcileResultView 的执行上下文数据结构或业务契约。
     * @param requeued requeued 对应的数据字段
     */
    record ReconcileResultView(int requeued) {
    }
}
