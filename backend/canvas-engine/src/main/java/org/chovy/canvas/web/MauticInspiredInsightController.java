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

@RestController
@RequestMapping("/canvas/mautic-insights")
@RequiredArgsConstructor
public class MauticInspiredInsightController {

    private final MauticInspiredInsightService service;

    @GetMapping("/audience-membership")
    public Mono<R<MauticInspiredInsightService.AudienceMembershipReport>> audienceMembership(
            @RequestParam Long audienceId,
            @RequestParam String userId) {
        return Mono.fromCallable(() -> R.ok(service.explainAudienceMembership(audienceId, userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/journey-path")
    public Mono<R<MauticInspiredInsightService.JourneyPathReport>> journeyPath(
            @RequestParam String executionId) {
        return Mono.fromCallable(() -> R.ok(service.explainJourneyPath(executionId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/channel-preference")
    public Mono<R<MauticInspiredInsightService.ChannelPreferenceReport>> channelPreference(
            @RequestParam String userId,
            @RequestParam(required = false) String preferredChannel) {
        return Mono.fromCallable(() -> R.ok(service.resolveChannelPreference(userId, preferredChannel)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/suppression-timeline")
    public Mono<R<MauticInspiredInsightService.SuppressionTimeline>> suppressionTimeline(
            @RequestParam String userId) {
        return Mono.fromCallable(() -> R.ok(service.suppressionTimeline(userId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/publish-health")
    public Mono<R<MauticInspiredInsightService.PublishHealthReport>> publishHealth(
            @RequestParam Long canvasId) {
        return Mono.fromCallable(() -> R.ok(service.publishHealth(canvasId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/frequency-templates")
    public Mono<R<List<MauticInspiredInsightService.FrequencyTemplate>>> frequencyTemplates() {
        return Mono.fromCallable(() -> R.ok(service.frequencyTemplates()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
