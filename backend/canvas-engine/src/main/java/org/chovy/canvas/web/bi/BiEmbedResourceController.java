package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResource;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardResourceService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateService;
import org.chovy.canvas.domain.bi.dashboard.BiDashboardRuntimeStateView;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketPayload;
import org.chovy.canvas.domain.bi.embed.BiEmbedTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi/embed/resources")
public class BiEmbedResourceController {

    private final BiEmbedTicketService embedTicketService;
    private final BiDashboardResourceService dashboardResourceService;
    private final BiDashboardRuntimeStateService runtimeStateService;

    public BiEmbedResourceController(BiEmbedTicketService embedTicketService,
                                     BiDashboardResourceService dashboardResourceService) {
        this(embedTicketService, dashboardResourceService, null);
    }

    @Autowired
    public BiEmbedResourceController(BiEmbedTicketService embedTicketService,
                                     BiDashboardResourceService dashboardResourceService,
                                     BiDashboardRuntimeStateService runtimeStateService) {
        this.embedTicketService = embedTicketService;
        this.dashboardResourceService = dashboardResourceService;
        this.runtimeStateService = runtimeStateService;
    }

    @PostMapping("/dashboard")
    public Mono<R<BiDashboardResource>> getDashboardResource(
            @RequestBody EmbedDashboardResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null) {
                        throw new IllegalArgumentException("embed dashboard resource request is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforceDashboardResourceScope(preview, request);
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforceDashboardResourceScope(payload, request);
                    return R.ok(dashboardResourceService.get(payload.tenantId(), payload.resourceKey()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/dashboard/runtime-state")
    public Mono<R<BiDashboardRuntimeStateView>> getDashboardRuntimeState(
            @RequestBody EmbedDashboardResourceRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            @RequestHeader(value = "Referer", required = false) String referer) {
        return Mono.fromCallable(() -> {
                    if (request == null) {
                        throw new IllegalArgumentException("embed dashboard resource request is required");
                    }
                    BiEmbedTicketPayload preview = embedTicketService.verify(request.ticket());
                    enforceDashboardResourceScope(preview, request);
                    BiEmbedTicketPayload payload = embedTicketService.verifyForUse(
                            request.ticket(),
                            origin == null || origin.isBlank() ? referer : origin);
                    enforceDashboardResourceScope(payload, request);
                    return R.ok(requireRuntimeStateService().get(
                            payload.tenantId(),
                            payload.username(),
                            payload.resourceKey()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void enforceDashboardResourceScope(BiEmbedTicketPayload payload, EmbedDashboardResourceRequest request) {
        if (payload == null) {
            throw new SecurityException("BI embed ticket is required");
        }
        if (!"DASHBOARD".equalsIgnoreCase(payload.resourceType())) {
            throw new SecurityException("BI embed resource only supports dashboard tickets");
        }
        if (!equalsIgnoreCase(payload.resourceType(), request.resourceType())
                || !payload.resourceKey().equals(request.resourceKey())) {
            throw new SecurityException("BI embed dashboard resource does not match ticket");
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    private BiDashboardRuntimeStateService requireRuntimeStateService() {
        if (runtimeStateService == null) {
            throw new IllegalStateException("BI dashboard runtime state service is required");
        }
        return runtimeStateService;
    }

    public record EmbedDashboardResourceRequest(
            String ticket,
            String resourceType,
            String resourceKey
    ) {
    }
}
