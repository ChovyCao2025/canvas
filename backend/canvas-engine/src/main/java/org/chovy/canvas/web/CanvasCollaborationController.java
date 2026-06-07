package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.collaboration.CanvasCollaborationSummaryService;
import org.chovy.canvas.domain.collaboration.UserWorkspacePreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
public class CanvasCollaborationController {

    private final CanvasCollaborationSummaryService summaryService;
    private final UserWorkspacePreferenceService preferenceService;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping("/{canvasId}/collaboration/summary")
    public Mono<R<CanvasCollaborationSummaryService.Summary>> summary(@PathVariable Long canvasId) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> summaryService.summary(context.tenantId(), canvasId))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @GetMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> editorPreference() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> preferenceService.getEditorPreference(
                                context.tenantId(), context.username()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @PutMapping("/preferences/editor")
    public Mono<R<UserWorkspacePreferenceService.Preference>> upsertEditorPreference(
            @RequestBody Map<String, Object> patch) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> upsert(context, patch))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    private UserWorkspacePreferenceService.Preference upsert(TenantContext context, Map<String, Object> patch) {
        return preferenceService.upsertEditorPreference(context.tenantId(), context.username(), patch);
    }
}
