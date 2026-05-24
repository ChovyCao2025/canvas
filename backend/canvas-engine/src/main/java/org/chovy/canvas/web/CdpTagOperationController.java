package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;

@RestController
@RequestMapping("/cdp/tag-operations")
@RequiredArgsConstructor
public class CdpTagOperationController {

    private final CdpTagOperationService service;

    @PostMapping
    public Mono<R<CdpTagOperationDO>> create(@RequestBody CdpBatchTagReq req) {
        return Mono.fromCallable(() -> R.ok(service.create(req)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping
    public Mono<R<List<CdpTagOperationDO>>> list(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int limit) {
        return Mono.fromCallable(() -> R.ok(service.listRecent(limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<R<CdpTagOperationDO>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.get(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/retry-failed")
    public Mono<R<CdpTagOperationDO>> retryFailed(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(service.retryFailed(id, null)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
