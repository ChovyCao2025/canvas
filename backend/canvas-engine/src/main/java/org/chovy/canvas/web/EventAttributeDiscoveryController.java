package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.EventAttrDefinitionDO;
import org.chovy.canvas.domain.cdp.EventAttributeDiscoveryService;
import org.chovy.canvas.dto.cdp.CdpDiscoveredAttributeDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/event-attributes")
@RequiredArgsConstructor
public class EventAttributeDiscoveryController {
    private final TenantContextResolver tenantContextResolver;
    private final EventAttributeDiscoveryService discoveryService;

    @GetMapping("/discovered")
    public Mono<R<List<CdpDiscoveredAttributeDTO>>> listDiscovered(
            @RequestParam(required = false) String status) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(discoveryService.list(tenantId(ctx), status)
                                .stream()
                                .map(this::toDto)
                                .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private CdpDiscoveredAttributeDTO toDto(EventAttrDefinitionDO row) {
        return new CdpDiscoveredAttributeDTO(
                row.getId(),
                row.getEventCode(),
                row.getAttrName(),
                row.getAttrType(),
                row.getStatus(),
                row.getSampleValue(),
                row.getFirstSeenAt(),
                row.getLastSeenAt());
    }

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }
}
