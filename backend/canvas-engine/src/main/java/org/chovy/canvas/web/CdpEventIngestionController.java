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

/**
 * CdpEventIngestionController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/cdp/events")
@RequiredArgsConstructor
public class CdpEventIngestionController {
    private final CdpWriteKeyAuthService writeKeyAuthService;
    private final CdpEventIngestionService ingestionService;
    /**
     * 处理 CDP 事件采集 请求接口，对应 POST /track。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 主要委托 writeKeyAuthService.authenticate, ingestionService.ingestBatch 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param request 请求体。
     * @param body 请求体。
     * @return 异步返回统一响应，包含处理 CDP 事件采集 请求后的业务数据。
     */
    @PostMapping("/track")
    public Mono<R<IngestionResult>> track(ServerHttpRequest request, @RequestBody Mono<BatchTrackReq> body) {
        return Mono.fromCallable(() -> writeKeyAuthService.authenticate(request.getHeaders()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(key -> body.defaultIfEmpty(new BatchTrackReq(List.of(), null))
                        .flatMap(req -> Mono.fromCallable(() -> R.ok(ingestionService.ingestBatch(key, req)))
                                .subscribeOn(Schedulers.boundedElastic())));
    }
}
