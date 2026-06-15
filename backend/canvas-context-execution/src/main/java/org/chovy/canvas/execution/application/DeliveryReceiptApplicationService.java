package org.chovy.canvas.execution.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.execution.api.DeliveryReceiptFacade;
import org.springframework.stereotype.Service;

@Service
public class DeliveryReceiptApplicationService implements DeliveryReceiptFacade {

    private final AtomicLong ids = new AtomicLong(5000);
    private final ObjectMapper objectMapper;

    public DeliveryReceiptApplicationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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

    private String rawPayloadJson(ReceiptCommand command) {
        try {
            return objectMapper.writeValueAsString(command.rawPayload());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("rawPayload is not JSON serializable", exception);
        }
    }
}
