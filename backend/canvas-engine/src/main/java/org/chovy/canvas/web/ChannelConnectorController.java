package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.ChannelConnectorDO;
import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;
import org.chovy.canvas.dal.dataobject.ChannelFallbackDecisionDO;
import org.chovy.canvas.dal.dataobject.ChannelProviderLimitDO;
import org.chovy.canvas.dal.mapper.ChannelConnectorMapper;
import org.chovy.canvas.dal.mapper.ChannelDedupeRecordMapper;
import org.chovy.canvas.dal.mapper.ChannelFallbackDecisionMapper;
import org.chovy.canvas.dal.mapper.ChannelProviderLimitMapper;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.ChannelFallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/channels/connectors")
public class ChannelConnectorController {

    private final Service service;
    private final TenantContextResolver tenantContextResolver;

    public ChannelConnectorController(Service service) {
        this(service, null);
    }

    @Autowired
    public ChannelConnectorController(ChannelConnectorMapper connectorMapper,
                                      ChannelProviderLimitMapper limitMapper,
                                      ChannelFallbackDecisionMapper decisionMapper,
                                      ChannelDedupeRecordMapper dedupeRecordMapper,
                                      ChannelFallbackService fallbackService,
                                      TenantContextResolver tenantContextResolver) {
        this(new MapperBackedService(connectorMapper, limitMapper, decisionMapper, dedupeRecordMapper, fallbackService),
                tenantContextResolver);
    }

    private ChannelConnectorController(Service service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<ConnectorRow>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/limits")
    public Mono<R<List<LimitRow>>> limits() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listLimits(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/mode")
    public Mono<R<Void>> updateMode(@PathVariable Long id, @RequestBody ModeUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.updateMode(tenantId, id, req.mode(), req.reason());
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/health-test")
    public Mono<R<HealthResult>> testHealth(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.testHealth(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/fallback/validate")
    public Mono<R<ValidationResult>> validateFallback(@RequestBody FallbackPolicyReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.validateFallback(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/fallback/decisions")
    public Mono<R<List<FallbackDecisionRow>>> decisions() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDecisions(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/dedupe-records")
    public Mono<R<List<DedupeRecordRow>>> dedupeRecords() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDedupeRecords(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    public interface Service {
        List<ConnectorRow> list(Long tenantId);

        List<LimitRow> listLimits(Long tenantId);

        void updateMode(Long tenantId, Long id, String mode, String reason);

        HealthResult testHealth(Long tenantId, Long id);

        ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req);

        List<FallbackDecisionRow> listDecisions(Long tenantId);

        List<DedupeRecordRow> listDedupeRecords(Long tenantId);
    }

    public record ConnectorRow(
            Long id,
            String connectorKey,
            String channel,
            String provider,
            String mode,
            String healthStatus,
            String healthMessage) {
    }

    public record LimitRow(
            String channel,
            String provider,
            String operation,
            Integer perSecondLimit,
            Long dailyLimit,
            boolean failClosed,
            String updatedAt) {
    }

    public record ModeUpdateReq(String mode, String reason) {
    }

    public record HealthResult(String status, String message) {
    }

    public record FallbackPolicyReq(String channel, String provider, String fallbackChannel, String fallbackProvider) {
    }

    public record ValidationResult(boolean valid, String message) {
    }

    public record FallbackDecisionRow(
            String originalChannel,
            String originalProvider,
            String finalChannel,
            String finalProvider,
            String decisionReason,
            String createdAt) {
    }

    public record DedupeRecordRow(String dedupeGroup, String contentHash, String channel, String userId, String expiresAt) {
    }

    private static class MapperBackedService implements Service {

        private final ChannelConnectorMapper connectorMapper;
        private final ChannelProviderLimitMapper limitMapper;
        private final ChannelFallbackDecisionMapper decisionMapper;
        private final ChannelDedupeRecordMapper dedupeRecordMapper;
        private final ChannelFallbackService fallbackService;

        private MapperBackedService(ChannelConnectorMapper connectorMapper,
                                    ChannelProviderLimitMapper limitMapper,
                                    ChannelFallbackDecisionMapper decisionMapper,
                                    ChannelDedupeRecordMapper dedupeRecordMapper,
                                    ChannelFallbackService fallbackService) {
            this.connectorMapper = connectorMapper;
            this.limitMapper = limitMapper;
            this.decisionMapper = decisionMapper;
            this.dedupeRecordMapper = dedupeRecordMapper;
            this.fallbackService = fallbackService;
        }

        @Override
        public List<ConnectorRow> list(Long tenantId) {
            return connectorMapper.selectList(new LambdaQueryWrapper<ChannelConnectorDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelConnectorDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelConnectorDO::getTenantId, 0L)
                            .orderByAsc(ChannelConnectorDO::getChannel, ChannelConnectorDO::getProvider))
                    .stream()
                    .map(row -> new ConnectorRow(
                            row.getId(),
                            row.getConnectorKey(),
                            row.getChannel(),
                            row.getProvider(),
                            row.getMode(),
                            row.getHealthStatus(),
                            row.getHealthMessage()))
                    .toList();
        }

        @Override
        public List<LimitRow> listLimits(Long tenantId) {
            return limitMapper.selectList(new LambdaQueryWrapper<ChannelProviderLimitDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelProviderLimitDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelProviderLimitDO::getTenantId, 0L)
                            .orderByAsc(ChannelProviderLimitDO::getChannel, ChannelProviderLimitDO::getProvider))
                    .stream()
                    .map(row -> new LimitRow(
                            row.getChannel(),
                            row.getProvider(),
                            row.getOperation(),
                            row.getPerSecondLimit(),
                            row.getDailyLimit(),
                            row.getFailClosed() == null || row.getFailClosed() == 1,
                            format(row.getUpdatedAt())))
                    .toList();
        }

        @Override
        public void updateMode(Long tenantId, Long id, String mode, String reason) {
            ChannelConnectorDO row = requireConnector(tenantId, id);
            ChannelConnector.ConnectorMode parsedMode = parseMode(mode);
            row.setMode(parsedMode.name());
            row.setDisabledReason(parsedMode == ChannelConnector.ConnectorMode.DISABLED ? reason : null);
            connectorMapper.updateById(row);
        }

        @Override
        public HealthResult testHealth(Long tenantId, Long id) {
            ChannelConnectorDO row = requireConnector(tenantId, id);
            ChannelConnector.ConnectorMode mode = parseMode(row.getMode());
            String status = switch (mode) {
                case SANDBOX -> "UP";
                case DISABLED -> "DISABLED";
                case REAL -> row.getHealthStatus() == null ? "UNKNOWN" : row.getHealthStatus();
            };
            String message = switch (mode) {
                case SANDBOX -> "sandbox connector ready";
                case DISABLED -> row.getDisabledReason();
                case REAL -> row.getHealthMessage();
            };
            row.setHealthStatus(status);
            row.setHealthMessage(message);
            row.setLastCheckedAt(LocalDateTime.now());
            connectorMapper.updateById(row);
            return new HealthResult(status, message);
        }

        @Override
        public ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req) {
            try {
                fallbackService.validateCandidate(tenantId, req.channel(), req.provider(), req.fallbackChannel(), req.fallbackProvider());
                return new ValidationResult(true, "ok");
            } catch (IllegalArgumentException ex) {
                return new ValidationResult(false, ex.getMessage());
            }
        }

        @Override
        public List<FallbackDecisionRow> listDecisions(Long tenantId) {
            return decisionMapper.selectList(new LambdaQueryWrapper<ChannelFallbackDecisionDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelFallbackDecisionDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelFallbackDecisionDO::getTenantId, 0L)
                            .orderByDesc(ChannelFallbackDecisionDO::getCreatedAt)
                            .last("LIMIT 50"))
                    .stream()
                    .map(row -> new FallbackDecisionRow(
                            row.getOriginalChannel(),
                            row.getOriginalProvider(),
                            row.getFinalChannel(),
                            row.getFinalProvider(),
                            row.getDecisionReason(),
                            format(row.getCreatedAt())))
                    .toList();
        }

        @Override
        public List<DedupeRecordRow> listDedupeRecords(Long tenantId) {
            return dedupeRecordMapper.selectList(new LambdaQueryWrapper<ChannelDedupeRecordDO>()
                            .in(tenantId != null && tenantId != 0L, ChannelDedupeRecordDO::getTenantId, List.of(0L, tenantId))
                            .eq(tenantId == null || tenantId == 0L, ChannelDedupeRecordDO::getTenantId, 0L)
                            .orderByDesc(ChannelDedupeRecordDO::getCreatedAt)
                            .last("LIMIT 50"))
                    .stream()
                    .map(row -> new DedupeRecordRow(
                            row.getDedupeGroup(),
                            row.getContentHash(),
                            row.getChannel(),
                            row.getUserId(),
                            format(row.getExpiresAt())))
                    .toList();
        }

        private ChannelConnectorDO requireConnector(Long tenantId, Long id) {
            ChannelConnectorDO row = connectorMapper.selectById(id);
            if (row == null) {
                throw new IllegalArgumentException("Channel connector not found: " + id);
            }
            Long effectiveTenant = tenantId == null ? 0L : tenantId;
            if (effectiveTenant != 0L && row.getTenantId() != null && !row.getTenantId().equals(effectiveTenant)) {
                throw new IllegalArgumentException("Channel connector tenant mismatch: " + id);
            }
            return row;
        }

        private static ChannelConnector.ConnectorMode parseMode(String mode) {
            if (mode == null || mode.isBlank()) {
                return ChannelConnector.ConnectorMode.DISABLED;
            }
            return ChannelConnector.ConnectorMode.valueOf(mode.trim().toUpperCase());
        }

        private static String format(LocalDateTime value) {
            return value == null ? null : value.toString();
        }
    }
}
