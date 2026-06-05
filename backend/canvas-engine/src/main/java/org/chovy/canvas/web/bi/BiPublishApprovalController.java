package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalRequestCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalReviewCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/bi/resources/publish-approvals")
public class BiPublishApprovalController {

    private final TenantContextResolver tenantContextResolver;
    private final BiPublishApprovalService approvalService;

    public BiPublishApprovalController(TenantContextResolver tenantContextResolver,
                                       BiPublishApprovalService approvalService) {
        this.tenantContextResolver = tenantContextResolver;
        this.approvalService = approvalService;
    }

    @GetMapping("")
    public Mono<R<List<BiPublishApprovalView>>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.listApprovals(
                                tenantId(context), resourceType, resourceKey, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("")
    public Mono<R<BiPublishApprovalView>> requestApproval(
            @RequestBody BiPublishApprovalRequestCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.requestApproval(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{approvalId}/review")
    public Mono<R<BiPublishApprovalView>> reviewApproval(
            @PathVariable Long approvalId,
            @RequestBody BiPublishApprovalReviewCommand command) {
        BiPublishApprovalReviewCommand merged = new BiPublishApprovalReviewCommand(
                approvalId,
                command == null ? null : command.status(),
                command == null ? null : command.reviewComment());
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.reviewApproval(
                                tenantId(context), username(context), merged)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
    }

    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    private String username(TenantContext context) {
        return context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
