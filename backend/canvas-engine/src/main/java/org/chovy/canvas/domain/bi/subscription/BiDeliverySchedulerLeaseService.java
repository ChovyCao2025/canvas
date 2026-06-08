package org.chovy.canvas.domain.bi.subscription;

import org.chovy.canvas.dal.dataobject.BiDeliverySchedulerLeaseDO;
import org.chovy.canvas.dal.mapper.BiDeliverySchedulerLeaseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BiDeliverySchedulerLeaseService 编排 domain.bi.subscription 场景的领域业务规则。
 */
@Service
public class BiDeliverySchedulerLeaseService {

    private final BiDeliverySchedulerLeaseMapper leaseMapper;
    private final String ownerId;

    /**
     * 创建 BiDeliverySchedulerLeaseService 实例并注入 domain.bi.subscription 场景依赖。
     * @param leaseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param configuredOwnerId 业务对象 ID，用于定位具体记录。
     */
    public BiDeliverySchedulerLeaseService(BiDeliverySchedulerLeaseMapper leaseMapper,
                                           @Value("${canvas.bi.delivery.scheduler.lease-owner-id:}") String configuredOwnerId) {
        this.leaseMapper = leaseMapper;
        this.ownerId = normalizeOwner(configuredOwnerId);
    }

    /**
     * 处理 BI 订阅运行态能力，串联计划、快照渲染、附件和投递审计。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param leaseKey leaseKey 参数，用于限定本次 BI 业务操作的输入范围
     * @param ttl ttl 参数，用于限定本次 BI 业务操作的输入范围
     * @return 如果状态更新成功返回 true，否则返回 false
     */
    public boolean acquire(Long tenantId, String leaseKey, Duration ttl) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedLeaseKey = required(leaseKey, "leaseKey");
        Duration scopedTtl = validateTtl(ttl);
        LocalDateTime now = LocalDateTime.now();
        BiDeliverySchedulerLeaseDO row = new BiDeliverySchedulerLeaseDO();
        row.setTenantId(scopedTenantId);
        row.setLeaseKey(scopedLeaseKey);
        row.setOwnerId(ownerId);
        row.setLastAcquiredAt(now);
        row.setLeaseUntil(now.plus(scopedTtl));
        leaseMapper.tryAcquire(row, now);
        return ownsLease(scopedTenantId, scopedLeaseKey, now);
    }

    /**
     * 处理 BI 订阅运行态能力，串联计划、快照渲染、附件和投递审计。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param leaseKey leaseKey 参数，用于限定本次 BI 业务操作的输入范围
     */
    public void release(Long tenantId, String leaseKey) {
        leaseMapper.release(
                normalizeTenant(tenantId),
                required(leaseKey, "leaseKey"),
                ownerId,
                LocalDateTime.now());
    }

    /**
     * 执行 ownerId 流程，围绕 owner id 完成校验、计算或结果组装。
     *
     * @return 返回 owner id 生成的文本或业务键。
     */
    String ownerId() {
        return ownerId;
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
        // tryAcquire 可能因并发竞争未真正持有租约，必须回读 owner 和过期时间确认本实例拥有执行权。
        BiDeliverySchedulerLeaseDO current = leaseMapper.findByKey(tenantId, leaseKey);
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
        // TTL 必须为正数，避免写入立即过期或永久有效的租约导致调度重复或停摆。
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
        // 未配置 owner 时生成进程级唯一值，保证多实例之间可以区分租约持有人。
        return "bi-delivery-" + UUID.randomUUID();
    }
}
