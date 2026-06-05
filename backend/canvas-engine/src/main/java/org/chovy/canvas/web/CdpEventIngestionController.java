package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.cdp.CdpEventIngestionService;
import org.chovy.canvas.domain.cdp.CdpWriteKeyAuthService;
import org.chovy.canvas.dto.cdp.BatchTrackReq;
import org.chovy.canvas.dto.cdp.IngestionResult;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/cdp/events")
@RequiredArgsConstructor
public class CdpEventIngestionController {
    private final CdpWriteKeyAuthService writeKeyAuthService;
    private final CdpEventIngestionService ingestionService;

    @PostMapping("/track")
    public Mono<R<IngestionResult>> track(ServerHttpRequest request, @RequestBody Mono<BatchTrackReq> body) {
        return Mono.fromCallable(() -> writeKeyAuthService.authenticate(request.getHeaders()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(key -> body.defaultIfEmpty(new BatchTrackReq(List.of(), null))
                        .flatMap(req -> Mono.fromCallable(() -> R.ok(ingestionService.ingestBatch(key, req)))
                                .subscribeOn(Schedulers.boundedElastic())));
    }
}
