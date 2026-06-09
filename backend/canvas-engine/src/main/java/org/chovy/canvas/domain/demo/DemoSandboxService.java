package org.chovy.canvas.domain.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
/**
 * DemoSandboxService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class DemoSandboxService {

    private static final int MAX_TTL_DAYS = 90;

    private final SandboxRepository repository;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 DemoSandboxService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public DemoSandboxService(SandboxRepository repository) {
        this(repository, Clock.systemUTC());
    }

    /**
     * 初始化 DemoSandboxService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public DemoSandboxService(SandboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param demoName 名称文本，用于展示或唯一性校验。
     * @param ttlDays ttl days 参数，用于 install 流程中的校验、计算或对象转换。
     * @return 返回 install 流程生成的业务结果。
     */
    public Sandbox install(Long tenantId, String demoName, int ttlDays) {
        requireTenantId(tenantId);
        requireText(demoName, "demo name is required");
        if (ttlDays < 1 || ttlDays > MAX_TTL_DAYS) {
            throw new IllegalArgumentException("ttlDays must be between 1 and 90");
        }
        Sandbox sandbox = new Sandbox(
                tenantId,
                demoName.trim(),
                "DEMO_TENANT_" + tenantId,
                "ACTIVE",
                clock.instant().plus(ttlDays, ChronoUnit.DAYS),
                null);
        repository.upsert(sandbox);
        return sandbox;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 reset 流程生成的业务结果。
     */
    public ResetResult reset(Long tenantId, String operator) {
        requireTenantId(tenantId);
        requireText(operator, "operator is required");
        Sandbox sandbox = repository.get(tenantId);
        if (sandbox == null) {
            throw new IllegalStateException("sandbox tenant " + tenantId + " is not installed");
        }
        Instant resetAt = clock.instant();
        repository.recordReset(tenantId, operator.trim(), resetAt);
        return new ResetResult(tenantId, sandbox.demoMarker(), resetAt);
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @return 返回 expired 汇总后的集合、分页或映射视图。
     */
    public List<Sandbox> expired() {
        return repository.findExpired(clock.instant());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private static void requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenant id is required");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Sandbox 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Sandbox(
            Long tenantId,
            String demoName,
            String demoMarker,
            String status,
            Instant expiresAt,
            Instant lastResetAt) {
    }

    /**
     * ResetResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ResetResult(Long tenantId, String demoMarker, Instant resetAt) {
    }

    /**
     * SandboxRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface SandboxRepository {
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param sandbox sandbox 参数，用于 upsert 流程中的校验、计算或对象转换。
         */
        void upsert(Sandbox sandbox);

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @return 返回 get 流程生成的业务结果。
         */
        Sandbox get(Long tenantId);

        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param operator 操作人标识，用于审计和权限判断。
         * @param resetAt 时间参数，用于计算窗口、过期或审计时间。
         */
        void recordReset(Long tenantId, String operator, Instant resetAt);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param now 时间参数，用于计算窗口、过期或审计时间。
         * @return 返回符合条件的数据列表或视图。
         */
        List<Sandbox> findExpired(Instant now);
    }
}
