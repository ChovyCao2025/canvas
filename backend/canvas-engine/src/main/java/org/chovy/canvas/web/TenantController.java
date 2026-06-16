package org.chovy.canvas.web;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.TenantDO;
import org.chovy.canvas.domain.compliance.AuditEventService;
import org.chovy.canvas.domain.tenant.TenantService;
import org.chovy.canvas.dto.tenant.TenantUsageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TenantController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    /** 承接租户基础资料、套餐和配额配置的管理逻辑。 */
    private final TenantService tenantService;
    /** 解析当前请求的租户上下文，保证接口按租户隔离读写。 */
    private final TenantContextResolver tenantContextResolver;
    /** 记录租户管理动作的审计事件，支撑安全追溯。 */
    private AuditEventService auditEventService;

    /**
     * 可选注入审计事件服务，用于记录租户启停、配额调整等管理动作。
     *
     * <p>该依赖在轻量测试或审计模块未装配时可以为空，业务接口会在记录审计前做空值保护，
     * 因此这里仅保存 Spring 容器提供的实例，不改变租户操作的权限判断。</p>
     *
     * @param auditEventService 合规审计服务实例，缺失时表示当前运行环境不落库审计事件。
     */
    @Autowired(required = false)
    public void setAuditEventService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }
    /**
     * 查询平台内全部租户。
     *
     * <p>该接口仅允许超级管理员访问，用于租户管理后台展示租户基础资料、套餐和状态。
     * 查询只读，不会触发配额重算或状态变更。</p>
     *
     * @return 异步返回租户列表。
     */
    @GetMapping
    public Mono<R<List<TenantDO>>> list() {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(tenantService::list)
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 创建新的租户并初始化套餐配额。
     *
     * <p>接口要求超级管理员上下文，创建人写入当前登录用户名。成功创建后会记录租户创建审计事件，
     * 审计元数据包含租户 key、套餐和初始配额配置。</p>
     *
     * @param req 租户创建请求，包含名称、租户 key、套餐编码和配额 JSON。
     * @return 异步返回新创建的租户记录。
     */
    @PostMapping
    public Mono<R<TenantDO>> create(@RequestBody TenantCreateReq req) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(() -> {
                            TenantDO tenant = tenantService.create(
                                    req.getName(), req.getTenantKey(), req.getPlanCode(),
                                    req.getQuotaJson(), context.username());
                            recordTenantAudit(context, "tenant create", tenant, metadata(req));
                            return tenant;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }
    /**
     * 停用指定租户。
     *
     * <p>接口仅允许超级管理员调用。停用会阻止该租户继续使用平台能力，并写入租户停用审计事件；
     * 已有业务数据不会在此接口中删除。</p>
     *
     * @param id 待停用的租户 ID。
     * @return 异步返回空响应，表示租户已停用。
     */
    @PutMapping("/{id}/disable")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() -> {
                                tenantService.disable(id, context.username());
                                recordTenantAudit(context, "tenant disable", id, Map.of());
                            })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }
    /**
     * 重新启用指定租户。
     *
     * <p>接口仅允许超级管理员调用。启用会恢复租户的平台访问能力，并记录租户启用审计事件；
     * 套餐和配额沿用租户当前配置。</p>
     *
     * @param id 待启用的租户 ID。
     * @return 异步返回空响应，表示租户已启用。
     */
    @PutMapping("/{id}/activate")
    public Mono<R<Void>> activate(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.<Void>fromRunnable(() -> {
                                tenantService.activate(id, context.username());
                                recordTenantAudit(context, "tenant activate", id, Map.of());
                            })
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }
    /**
     * 查询指定租户的资源使用量。
     *
     * <p>接口仅允许超级管理员访问，用于核对租户当前配额消耗和套餐容量。
     * 查询只读取统计结果，不会改变配额或触发补偿计算。</p>
     *
     * @param id 租户 ID。
     * @return 异步返回租户使用量视图。
     */
    @GetMapping("/{id}/usage")
    public Mono<R<TenantUsageDTO>> usage(@PathVariable Long id) {
        return requireSuperAdmin()
                .flatMap(context -> Mono.fromCallable(() -> tenantService.usage(id))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireSuperAdmin 流程生成的业务结果。
     */
    private Mono<TenantContext> requireSuperAdmin() {
        return tenantContextResolver.current()
                .filter(TenantContext::isSuperAdmin)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "需要超级管理员权限")));
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param tenant tenant 参数，用于 recordTenantAudit 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 recordTenantAudit 流程中的校验、计算或对象转换。
     */
    private void recordTenantAudit(TenantContext context,
                                   String operation,
                                   TenantDO tenant,
                                   Map<String, Object> metadata) {
        recordTenantAudit(context, operation, tenant == null ? null : tenant.getId(), metadata);
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param metadata metadata 参数，用于 recordTenantAudit 流程中的校验、计算或对象转换。
     */
    private void recordTenantAudit(TenantContext context,
                                   String operation,
                                   Long tenantId,
                                   Map<String, Object> metadata) {
        if (auditEventService == null) {
            return;
        }
        auditEventService.record(AuditEventService.AuditEventCommand.builder()
                .tenantId(tenantId)
                .actor(context == null || context.username() == null ? "system" : context.username())
                .actorRole(context == null ? null : context.role())
                .operation(operation)
                .targetType("tenant")
                .targetId(tenantId == null ? "0" : String.valueOf(tenantId))
                .metadata(metadata == null ? Map.of() : metadata)
                .build());
    }

    /**
     * 根据租户创建请求组装审计元数据。
     *
     * @param req 租户创建请求
     * @return 审计事件使用的元数据映射
     */
    private Map<String, Object> metadata(TenantCreateReq req) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", req.getName());
        metadata.put("tenantKey", req.getTenantKey());
        metadata.put("planCode", req.getPlanCode());
        metadata.put("quotaPresent", req.getQuotaJson() != null && !req.getQuotaJson().isBlank());
        return metadata;
    }

    @Data
    /**
     * TenantCreateReq 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public static class TenantCreateReq {
        /** 租户或模板展示名称，用于管理界面识别业务对象。 */
        private String name;
        /** 租户唯一键，用于外部系统和平台内部的租户关联。 */
        private String tenantKey;
        /** 租户套餐编码，用于决定可用能力和默认资源配额。 */
        private String planCode;
        /** 租户配额 JSON，用于记录用量上限和功能限制。 */
        private String quotaJson;
    }
}
