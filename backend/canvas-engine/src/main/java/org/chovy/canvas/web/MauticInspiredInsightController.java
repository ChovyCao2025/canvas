package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.insights.MauticInspiredInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * MauticInspiredInsightController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/mautic-insights")
@RequiredArgsConstructor
public class MauticInspiredInsightController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final MauticInspiredInsightService service;
    /**
     * 处理 Mautic Inspired Insight 请求接口，对应 GET /audience-membership。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param audienceId audience ID。
     * @param userId user ID。
     * @return 异步返回统一响应，包含处理 Mautic Inspired Insight 请求后的业务数据。
     */
    @GetMapping("/audience-membership")
    public Mono<R<MauticInspiredInsightService.AudienceMembershipReport>> audienceMembership(
            @RequestParam Long audienceId,
            @RequestParam String userId) {
        return Mono.fromCallable(() -> R.ok(service.explainAudienceMembership(audienceId, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 Mautic Inspired Insight 请求接口，对应 GET /journey-path。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param executionId execution ID。
     * @return 异步返回统一响应，包含处理 Mautic Inspired Insight 请求后的业务数据。
     */
    @GetMapping("/journey-path")
    public Mono<R<MauticInspiredInsightService.JourneyPathReport>> journeyPath(
            @RequestParam String executionId) {
        return Mono.fromCallable(() -> R.ok(service.explainJourneyPath(executionId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 Mautic Inspired Insight 请求接口，对应 GET /channel-preference。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @param preferredChannel 请求参数，可选。
     * @return 异步返回统一响应，包含处理 Mautic Inspired Insight 请求后的业务数据。
     */
    @GetMapping("/channel-preference")
    public Mono<R<MauticInspiredInsightService.ChannelPreferenceReport>> channelPreference(
            @RequestParam String userId,
            @RequestParam(required = false) String preferredChannel) {
        return Mono.fromCallable(() -> R.ok(service.resolveChannelPreference(userId, preferredChannel)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 Mautic Inspired Insight 请求接口，对应 GET /suppression-timeline。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID。
     * @return 异步返回统一响应，包含处理 Mautic Inspired Insight 请求后的业务数据。
     */
    @GetMapping("/suppression-timeline")
    public Mono<R<MauticInspiredInsightService.SuppressionTimeline>> suppressionTimeline(
            @RequestParam String userId) {
        return Mono.fromCallable(() -> R.ok(service.suppressionTimeline(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 发布 Mautic Inspired Insight接口，对应 GET /publish-health。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param canvasId 画布 ID。
     * @return 异步返回统一响应，包含发布 Mautic Inspired Insight后的业务数据。
     */
    @GetMapping("/publish-health")
    public Mono<R<MauticInspiredInsightService.PublishHealthReport>> publishHealth(
            @RequestParam Long canvasId) {
        return Mono.fromCallable(() -> R.ok(service.publishHealth(canvasId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 处理 Mautic Inspired Insight 请求接口，对应 GET /frequency-templates。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/frequency-templates")
    public Mono<R<List<MauticInspiredInsightService.FrequencyTemplate>>> frequencyTemplates() {
        return Mono.fromCallable(() -> R.ok(service.frequencyTemplates()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
