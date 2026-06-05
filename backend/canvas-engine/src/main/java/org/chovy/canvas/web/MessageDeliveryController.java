package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.delivery.DeliveryOutboxDO;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReconciliationJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message-deliveries")
public class MessageDeliveryController {

    private final DeliveryOutboxService outboxService;
    private final DeliveryReconciliationJob reconciliationJob;

    public MessageDeliveryController(DeliveryOutboxService outboxService,
                                     DeliveryReconciliationJob reconciliationJob) {
        this.outboxService = outboxService;
        this.reconciliationJob = reconciliationJob;
    }

    @GetMapping
    public Mono<R<PageResult<DeliveryOutboxDO>>> list(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerMessageId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> R.ok(outboxService.search(new DeliveryOutboxService.DeliverySearchCriteria(
                        tenantId, canvasId, executionId, userId, channel, provider, status, providerMessageId, page, size))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<DeliveryOutboxDO>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> outboxService.findById(id)
                        .map(R::ok)
                        .orElseGet(() -> R.fail("message delivery not found: " + id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}/receipts")
    public Mono<R<List<DeliveryReceiptLog>>> receipts(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(outboxService.receiptHistory(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/replay")
    public Mono<R<Map<String, Object>>> replayDead(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            boolean replayed = outboxService.replayDead(id);
            if (!replayed) {
                return R.<Map<String, Object>>fail("delivery is not replayable: " + id);
            }
            return R.ok(Map.<String, Object>of("outboxId", id, "status", DeliveryOutboxService.STATUS_PENDING));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/reconcile")
    public Mono<R<Map<String, Object>>> reconcile() {
        return Mono.fromCallable(() -> {
            int requeued = reconciliationJob.reconcile();
            return R.ok(Map.<String, Object>of("requeued", requeued));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
