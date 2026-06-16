package org.chovy.canvas.execution.application;

import java.util.List;
import java.util.Optional;

import org.chovy.canvas.execution.api.MessageDeliveryFacade;
import org.chovy.canvas.execution.domain.MessageDeliveryCatalog;
import org.springframework.stereotype.Service;

/**
 * 定义 MessageDeliveryApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class MessageDeliveryApplicationService implements MessageDeliveryFacade {

    /**
     * 保存 catalog 对应的状态或配置。
     */
    private final MessageDeliveryCatalog catalog;

    /**
     * 执行 MessageDeliveryApplicationService 对应的业务处理。
     */
    public MessageDeliveryApplicationService() {
        this(new MessageDeliveryCatalog());
    }

    /**
     * 执行 MessageDeliveryApplicationService 对应的业务处理。
     * @param catalog catalog 参数
     */
    public MessageDeliveryApplicationService(MessageDeliveryCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行 search 对应的业务处理。
     * @param query query 参数
     * @return 处理后的结果
     */
    @Override
    public DeliveryPageView search(DeliverySearchQuery query) {
        return catalog.search(query);
    }

    /**
     * 执行 findById 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public Optional<MessageDeliveryCatalog.Delivery> findById(Long id) {
        return catalog.findById(id);
    }

    /**
     * 执行 receipts 对应的业务处理。
     * @param outboxId outboxId 参数
     * @return 处理后的结果
     */
    @Override
    public List<MessageDeliveryCatalog.Receipt> receipts(Long outboxId) {
        return catalog.receipts(outboxId);
    }

    /**
     * 执行 replay 对应的业务处理。
     * @param id id 参数
     * @return 处理后的结果
     */
    @Override
    public ReplayResultView replay(Long id) {
        return catalog.replay(id);
    }

    /**
     * 执行 reconcile 对应的业务处理。
     * @return 处理后的结果
     */
    @Override
    public ReconcileResultView reconcile() {
        return catalog.reconcile();
    }
}
