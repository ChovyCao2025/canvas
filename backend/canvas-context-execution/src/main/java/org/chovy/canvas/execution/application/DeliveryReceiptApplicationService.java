package org.chovy.canvas.execution.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.execution.api.DeliveryReceiptFacade;
import org.springframework.stereotype.Service;

/**
 * 定义 DeliveryReceiptApplicationService 的执行上下文数据结构或业务契约。
 */
@Service
public class DeliveryReceiptApplicationService implements DeliveryReceiptFacade {

    private final AtomicLong ids = new AtomicLong(5000);

    /**
     * 保存 objectMapper 对应的状态或配置。
     */
    private final ObjectMapper objectMapper;

    /**
     * 执行 DeliveryReceiptApplicationService 对应的业务处理。
     * @param objectMapper objectMapper 参数
     */
    public DeliveryReceiptApplicationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 recordReceipt 对应的业务处理。
     * @param command command 参数
     */
    @Override
    public ReceiptView recordReceipt(ReceiptCommand command) {
        return new ReceiptView(
                ids.incrementAndGet(),
                null,
                null,
                command.provider(),
                command.providerMessageId(),
                command.receiptType(),
                rawPayloadJson(command),
                command.idempotencyKey(),
                command.receivedAt(),
                LocalDateTime.now());
    }

    /**
     * 执行 rawPayloadJson 对应的业务处理。
     * @param command command 参数
     */
    private String rawPayloadJson(ReceiptCommand command) {
        try {
            return objectMapper.writeValueAsString(command.rawPayload());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("rawPayload is not JSON serializable", exception);
        }
    }
}
