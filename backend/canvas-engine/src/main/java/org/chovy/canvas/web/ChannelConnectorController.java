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
/**
 * ChannelConnectorController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class ChannelConnectorController {

    private final Service service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelConnectorController(Service service) {
        this(service, null);
    }

    @Autowired
    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param connectorMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param limitMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param decisionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dedupeRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fallbackService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ChannelConnectorController(ChannelConnectorMapper connectorMapper,
                                      ChannelProviderLimitMapper limitMapper,
                                      ChannelFallbackDecisionMapper decisionMapper,
                                      ChannelDedupeRecordMapper dedupeRecordMapper,
                                      ChannelFallbackService fallbackService,
                                      TenantContextResolver tenantContextResolver) {
        this(new MapperBackedService(connectorMapper, limitMapper, decisionMapper, dedupeRecordMapper, fallbackService),
                tenantContextResolver);
    }

    /**
     * 初始化 ChannelConnectorController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    private ChannelConnectorController(Service service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<ConnectorRow>>> list() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.list(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/limits")
    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @return 返回解析、归一化或安全处理后的值。
     */
    public Mono<R<List<LimitRow>>> limits() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listLimits(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/mode")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<Void>> updateMode(@PathVariable Long id, @RequestBody ModeUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.updateMode(tenantId, id, req.mode(), req.reason());
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/health-test")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 testHealth 流程生成的业务结果。
     */
    public Mono<R<HealthResult>> testHealth(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.testHealth(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/fallback/validate")
    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回布尔判断结果。
     */
    public Mono<R<ValidationResult>> validateFallback(@RequestBody FallbackPolicyReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.validateFallback(tenantId, req)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/fallback/decisions")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 decisions 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<FallbackDecisionRow>>> decisions() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDecisions(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/dedupe-records")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 dedupe records 汇总后的集合、分页或映射视图。
     */
    public Mono<R<List<DedupeRecordRow>>> dedupeRecords() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.listDedupeRecords(tenantId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 current tenant id 计算得到的数量、金额或指标值。
     */
    private Mono<Long> currentTenantId() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return tenantContextResolver.current()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    /**
     * Service 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public interface Service {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<ConnectorRow> list(Long tenantId);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<LimitRow> listLimits(Long tenantId);

        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @param mode mode 参数，用于 updateMode 流程中的校验、计算或对象转换。
         * @param reason 原因说明，用于记录状态变化的业务依据。
         */
        void updateMode(Long tenantId, Long id, String mode, String reason);

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 testHealth 流程生成的业务结果。
         */
        HealthResult testHealth(Long tenantId, Long id);

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param req 请求对象，承载本次操作的输入参数。
         * @return 返回布尔判断结果。
         */
        ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<FallbackDecisionRow> listDecisions(Long tenantId);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
        List<DedupeRecordRow> listDedupeRecords(Long tenantId);
    }

    /**
     * ConnectorRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ConnectorRow(
            Long id,
            String connectorKey,
            String channel,
            String provider,
            String mode,
            String healthStatus,
            String healthMessage) {
    }

    /**
     * LimitRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record LimitRow(
            String channel,
            String provider,
            String operation,
            Integer perSecondLimit,
            Long dailyLimit,
            boolean failClosed,
            String updatedAt) {
    }

    /**
     * ModeUpdateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ModeUpdateReq(String mode, String reason) {
    }

    /**
     * HealthResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record HealthResult(String status, String message) {
    }

    /**
     * FallbackPolicyReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record FallbackPolicyReq(String channel, String provider, String fallbackChannel, String fallbackProvider) {
    }

    /**
     * ValidationResult 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ValidationResult(boolean valid, String message) {
    }

    /**
     * FallbackDecisionRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record FallbackDecisionRow(
            String originalChannel,
            String originalProvider,
            String finalChannel,
            String finalProvider,
            String decisionReason,
            String createdAt) {
    }

    /**
     * DedupeRecordRow 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record DedupeRecordRow(String dedupeGroup, String contentHash, String channel, String userId, String expiresAt) {
    }

    /**
     * MapperBackedService 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    private static class MapperBackedService implements Service {

        private final ChannelConnectorMapper connectorMapper;
        private final ChannelProviderLimitMapper limitMapper;
        private final ChannelFallbackDecisionMapper decisionMapper;
        private final ChannelDedupeRecordMapper dedupeRecordMapper;
        private final ChannelFallbackService fallbackService;

        /**
         * 初始化 MapperBackedService 实例。
         *
         * @param connectorMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param limitMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param decisionMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param dedupeRecordMapper 依赖组件，用于完成数据访问或外部能力调用。
         * @param fallbackService 依赖组件，用于完成数据访问或外部能力调用。
         */
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
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
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
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
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
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @param mode mode 参数，用于 updateMode 流程中的校验、计算或对象转换。
         * @param reason 原因说明，用于记录状态变化的业务依据。
         */
        public void updateMode(Long tenantId, Long id, String mode, String reason) {
            ChannelConnectorDO row = requireConnector(tenantId, id);
            ChannelConnector.ConnectorMode parsedMode = parseMode(mode);
            row.setMode(parsedMode.name());
            row.setDisabledReason(parsedMode == ChannelConnector.ConnectorMode.DISABLED ? reason : null);
            connectorMapper.updateById(row);
        }

        @Override
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 testHealth 流程生成的业务结果。
         */
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
        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param req 请求对象，承载本次操作的输入参数。
         * @return 返回布尔判断结果。
         */
        public ValidationResult validateFallback(Long tenantId, FallbackPolicyReq req) {
            try {
                fallbackService.validateCandidate(tenantId, req.channel(), req.provider(), req.fallbackChannel(), req.fallbackProvider());
                return new ValidationResult(true, "ok");
            } catch (IllegalArgumentException ex) {
                return new ValidationResult(false, ex.getMessage());
            }
        }

        @Override
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
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
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回符合条件的数据列表或视图。
         */
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

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param id 业务对象 ID，用于定位具体记录。
         * @return 返回 requireConnector 流程生成的业务结果。
         */
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

        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param mode mode 参数，用于 parseMode 流程中的校验、计算或对象转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        private static ChannelConnector.ConnectorMode parseMode(String mode) {
            if (mode == null || mode.isBlank()) {
                return ChannelConnector.ConnectorMode.DISABLED;
            }
            return ChannelConnector.ConnectorMode.valueOf(mode.trim().toUpperCase());
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回 format 生成的文本或业务键。
         */
        private static String format(LocalDateTime value) {
            return value == null ? null : value.toString();
        }
    }
}
