package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacySubjectTombstoneDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacySubjectTombstoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehousePrivacyTombstoneService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyTombstoneService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String ERASURE_STATUS_PASS = "PASS";
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TEXT_LENGTH = 1000;

    private final CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper;
    private final CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper;
    private final Clock clock;

    /**
     * 初始化 CdpWarehousePrivacyTombstoneService 实例。
     *
     * @param tombstoneMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper) {
        this(tombstoneMapper, null, Clock.systemDefaultZone());
    }

    @Autowired
    /**
     * 初始化 CdpWarehousePrivacyTombstoneService 实例。
     *
     * @param tombstoneMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param erasureRequestMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyTombstoneService(
            CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
            CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper) {
        this(tombstoneMapper, erasureRequestMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehousePrivacyTombstoneService 实例。
     *
     * @param tombstoneMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
                                        Clock clock) {
        this(tombstoneMapper, null, clock);
    }

    /**
     * 初始化 CdpWarehousePrivacyTombstoneService 实例。
     *
     * @param tombstoneMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param erasureRequestMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehousePrivacyTombstoneService(CdpWarehousePrivacySubjectTombstoneMapper tombstoneMapper,
                                        CdpWarehousePrivacyErasureRequestMapper erasureRequestMapper,
                                        Clock clock) {
        this.tombstoneMapper = tombstoneMapper;
        this.erasureRequestMapper = erasureRequestMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public TombstoneView create(Long tenantId, TombstoneCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("tombstone command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String subjectType = upperDefault(command.subjectType(), "USER_ID");
        String subjectValue = required(command.subjectValue(), "subjectValue");
        String hash = subjectHash(scopedTenantId, subjectType, subjectValue);
        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, subjectType, hash);
        if (row == null) {
            row = new CdpWarehousePrivacySubjectTombstoneDO();
            row.setTenantId(scopedTenantId);
            row.setSubjectType(subjectType);
            row.setSubjectHash(hash);
            row.setSubjectRefMasked(mask(subjectValue));
            row.setBlockedEventCount(0L);
        }
        row.setStatus(STATUS_ACTIVE);
        row.setSourceRequestId(command.sourceRequestId());
        row.setSourceRequestKey(blankToNull(command.sourceRequestKey()));
        row.setReason(limit(required(command.reason(), "reason")));
        row.setCreatedBy(defaultString(command.createdBy(), "system"));
        row.setRevokedBy(null);
        row.setRevokedAt(null);
        if (row.getId() == null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            tombstoneMapper.insert(row);
        } else {
            tombstoneMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public TombstoneView createFromErasureRequest(Long tenantId, ErasureRequestTombstoneCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("erasure request tombstone command is required");
        }
        if (command.requestId() == null || command.requestId() <= 0) {
            throw new IllegalArgumentException("requestId must be positive");
        }
        if (erasureRequestMapper == null) {
            throw new IllegalStateException("privacy erasure request mapper is not configured");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehousePrivacyErasureRequestDO request = erasureRequestMapper.selectById(command.requestId());
        if (request == null || !scopedTenantId.equals(request.getTenantId())) {
            throw new IllegalArgumentException("privacy erasure request not found: " + command.requestId());
        }
        if (!ERASURE_STATUS_PASS.equals(normalizeStatus(request.getStatus()))) {
            throw new IllegalArgumentException("privacy erasure request must be PASS before tombstone creation");
        }
        String subjectType = upperDefault(request.getSubjectType(), "USER_ID");
        String subjectHash = required(request.getSubjectHash(), "subjectHash");
        String subjectRefMasked = required(request.getSubjectRefMasked(), "subjectRefMasked");

        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, subjectType, subjectHash);
        if (row == null) {
            row = new CdpWarehousePrivacySubjectTombstoneDO();
            row.setTenantId(scopedTenantId);
            row.setSubjectType(subjectType);
            row.setSubjectHash(subjectHash);
            row.setSubjectRefMasked(subjectRefMasked);
            row.setBlockedEventCount(0L);
        }
        row.setStatus(STATUS_ACTIVE);
        row.setSourceRequestId(request.getId());
        row.setSourceRequestKey(request.getRequestKey());
        row.setReason(limit(defaultString(command.reason(), request.getReason())));
        row.setCreatedBy(defaultString(command.createdBy(), defaultString(request.getRequestedBy(), "system")));
        row.setRevokedBy(null);
        row.setRevokedAt(null);
        if (row.getId() == null) {
            tombstoneMapper.insert(row);
        } else {
            tombstoneMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 revoke 流程生成的业务结果。
     */
    public TombstoneView revoke(Long tenantId, Long id, RevokeCommand command) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        CdpWarehousePrivacySubjectTombstoneDO row = tombstoneMapper.selectById(id);
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            throw new IllegalArgumentException("privacy tombstone not found: " + id);
        }
        row.setStatus(STATUS_REVOKED);
        row.setRevokedBy(defaultString(command == null ? null : command.revokedBy(), "system"));
        row.setRevokedAt(now());
        tombstoneMapper.updateById(row);
        return toView(row);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 decide 流程生成的业务结果。
     */
    public TombstoneDecision decide(Long tenantId, String subjectType, String subjectValue) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedType = upperDefault(subjectType, "USER_ID");
        String value = required(subjectValue, "subjectValue");
        String hash = subjectHash(scopedTenantId, normalizedType, value);
        CdpWarehousePrivacySubjectTombstoneDO row = findByHash(scopedTenantId, normalizedType, hash);
        boolean blocked = row != null && STATUS_ACTIVE.equals(normalizeStatus(row.getStatus()));
        return new TombstoneDecision(
                scopedTenantId,
                normalizedType,
                hash,
                mask(value),
                blocked,
                row == null ? null : row.getId(),
                row == null ? null : row.getSourceRequestKey(),
                blocked ? "subject is blocked by active privacy tombstone" : "subject is not tombstoned");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @param source source 参数，用于 enforceNotBlocked 流程中的校验、计算或对象转换。
     * @return 返回 enforceNotBlocked 流程生成的业务结果。
     */
    public TombstoneDecision enforceNotBlocked(Long tenantId,
                                               String subjectType,
                                               String subjectValue,
                                               String source) {
        TombstoneDecision decision = decide(tenantId, subjectType, subjectValue);
        if (!decision.blocked()) {
            return decision;
        }
        tombstoneMapper.recordBlocked(decision.tenantId(), decision.subjectType(),
                decision.subjectHash(), now());
        throw new PrivacyTombstoneViolationException(
                "privacy tombstone blocks " + defaultString(source, "subject") + ": " + decision.subjectRefMasked());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<TombstoneView> list(Long tenantId, String status, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO> query =
                new LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO>()
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacySubjectTombstoneDO::getId)
                        .last("LIMIT " + boundLimit(limit));
        if (hasText(status)) {
            query.eq(CdpWarehousePrivacySubjectTombstoneDO::getStatus,
                    status.trim().toUpperCase(Locale.ROOT));
        }
        List<CdpWarehousePrivacySubjectTombstoneDO> rows = tombstoneMapper.selectList(query);
        return rows == null ? List.of() : rows.stream().map(this::toView).toList();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectHash subject hash 参数，用于 findByHash 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehousePrivacySubjectTombstoneDO findByHash(Long tenantId,
                                                             String subjectType,
                                                             String subjectHash) {
        List<CdpWarehousePrivacySubjectTombstoneDO> rows = tombstoneMapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacySubjectTombstoneDO>()
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getTenantId, tenantId)
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getSubjectType, subjectType)
                        .eq(CdpWarehousePrivacySubjectTombstoneDO::getSubjectHash, subjectHash)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private TombstoneView toView(CdpWarehousePrivacySubjectTombstoneDO row) {
        return new TombstoneView(
                row.getId(),
                row.getTenantId(),
                row.getSubjectType(),
                row.getSubjectHash(),
                row.getSubjectRefMasked(),
                row.getStatus(),
                row.getSourceRequestId(),
                row.getSourceRequestKey(),
                row.getReason(),
                row.getBlockedEventCount() == null ? 0L : row.getBlockedEventCount(),
                row.getLastBlockedAt(),
                row.getCreatedBy(),
                row.getRevokedBy(),
                row.getRevokedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回 subject hash 生成的文本或业务键。
     */
    private String subjectHash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((tenantId + ":" + subjectType + ":" + subjectValue)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash privacy tombstone subject", e);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param subjectValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String mask(String subjectValue) {
        String value = required(subjectValue, "subjectValue");
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : STATUS_REVOKED;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 upperDefault 流程中的校验、计算或对象转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String fallback) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int value) {
        int limit = value <= 0 ? 20 : value;
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * TombstoneCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TombstoneCommand(
            String subjectType,
            String subjectValue,
            Long sourceRequestId,
            String sourceRequestKey,
            String reason,
            String createdBy) {
    }

    /**
     * RevokeCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RevokeCommand(String revokedBy) {
    }

    /**
     * ErasureRequestTombstoneCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ErasureRequestTombstoneCommand(Long requestId, String reason, String createdBy) {
    }

    /**
     * TombstoneDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TombstoneDecision(
            Long tenantId,
            String subjectType,
            String subjectHash,
            String subjectRefMasked,
            boolean blocked,
            Long tombstoneId,
            String sourceRequestKey,
            String reason) {
    }

    /**
     * TombstoneView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TombstoneView(
            Long id,
            Long tenantId,
            String subjectType,
            String subjectHash,
            String subjectRefMasked,
            String status,
            Long sourceRequestId,
            String sourceRequestKey,
            String reason,
            long blockedEventCount,
            LocalDateTime lastBlockedAt,
            String createdBy,
            String revokedBy,
            LocalDateTime revokedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /**
     * PrivacyTombstoneViolationException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class PrivacyTombstoneViolationException extends IllegalArgumentException {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         * @return 返回 PrivacyTombstoneViolationException 流程生成的业务结果。
         */
        public PrivacyTombstoneViolationException(String message) {
            super(message);
        }
    }
}
