package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseJobLeaseDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseJobLeaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CdpWarehouseJobLeaseService 编排 domain.warehouse 场景的领域业务规则。
 */
@Service
public class CdpWarehouseJobLeaseService {

    private final CdpWarehouseJobLeaseMapper leaseMapper;
    private final String ownerId;

    /**
     * 创建 CdpWarehouseJobLeaseService 实例并注入 domain.warehouse 场景依赖。
     * @param leaseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param configuredOwnerId 业务对象 ID，用于定位具体记录。
     */
    public CdpWarehouseJobLeaseService(CdpWarehouseJobLeaseMapper leaseMapper,
                                       @Value("${canvas.warehouse.lease.owner-id:}") String configuredOwnerId) {
        this.leaseMapper = leaseMapper;
        this.ownerId = normalizeOwner(configuredOwnerId);
    }

    /**
     * 在租户级租约保护下执行任务，作为CDP 数仓的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param leaseKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param ttl ttl 参数，用于 runWithLease 流程中的校验、计算或对象转换。
     * @param work 拿到租约后执行的业务逻辑，返回值会作为租约任务结果
     * @return 如果目标记录在租户边界内被成功更新或规则匹配则返回 true，否则返回 false
     */
    public boolean runWithLease(Long tenantId, String leaseKey, Duration ttl, Supplier<Boolean> work) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (work == null) {
            throw new IllegalArgumentException("work is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedLeaseKey = required(leaseKey, "leaseKey");
        Duration scopedTtl = validateTtl(ttl);
        LocalDateTime now = LocalDateTime.now();
        CdpWarehouseJobLeaseDO row = new CdpWarehouseJobLeaseDO();
        row.setTenantId(scopedTenantId);
        row.setLeaseKey(scopedLeaseKey);
        row.setOwnerId(ownerId);
        row.setLastAcquiredAt(now);
        row.setLeaseUntil(now.plus(scopedTtl));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        leaseMapper.tryAcquire(row, now);
        if (!ownsLease(scopedTenantId, scopedLeaseKey, now)) {
            return false;
        }
        try {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return Boolean.TRUE.equals(work.get());
        } finally {
            leaseMapper.release(scopedTenantId, scopedLeaseKey, ownerId, LocalDateTime.now());
        }
    }

    /**
     * 执行 ownsLease 流程，围绕 owns lease 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param leaseKey 业务键，用于在同一租户下定位资源。
     * @param acquiredAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 owns lease 的布尔判断结果。
     */
    private boolean ownsLease(Long tenantId, String leaseKey, LocalDateTime acquiredAt) {
        CdpWarehouseJobLeaseDO current = leaseMapper.findByKey(tenantId, leaseKey);
        return current != null
                && ownerId.equals(current.getOwnerId())
                && current.getLeaseUntil() != null
                && current.getLeaseUntil().isAfter(acquiredAt);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param ttl ttl 参数，用于 validateTtl 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private Duration validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return ttl;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param configuredOwnerId 业务对象 ID，用于定位具体记录。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOwner(String configuredOwnerId) {
        if (configuredOwnerId != null && !configuredOwnerId.isBlank()) {
            return configuredOwnerId.trim();
        }
        return "warehouse-" + UUID.randomUUID();
    }
}
