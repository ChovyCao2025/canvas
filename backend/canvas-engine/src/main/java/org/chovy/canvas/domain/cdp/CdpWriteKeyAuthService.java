package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpWriteKeyDO;
import org.chovy.canvas.dal.mapper.CdpWriteKeyMapper;
import org.chovy.canvas.dto.cdp.CdpWriteKeyCreateReq;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/**
 * CdpWriteKeyAuthService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWriteKeyAuthService {
    private static final int PREFIX_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CdpWriteKeyMapper writeKeyMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * AuthenticatedWriteKey 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AuthenticatedWriteKey(
            Long writeKeyId,
            Long tenantId,
            String keyPrefix,
            String platform,
            Integer rateLimitQps,
            Long dailyQuota
    ) {
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 authenticate 流程生成的业务结果。
     */
    public AuthenticatedWriteKey authenticate(HttpHeaders headers) {
        String raw = extractBasicWriteKey(headers);
        CdpWriteKeyDO row = writeKeyMapper.selectOne(new LambdaQueryWrapper<CdpWriteKeyDO>()
                .eq(CdpWriteKeyDO::getKeyPrefix, prefix(raw))
                .last("LIMIT 1"));
        if (row == null || !passwordEncoder.matches(raw, row.getKeyHash())) {
            throw unauthorized("CDP write key is invalid");
        }
        if (!CdpWriteKeyDO.ACTIVE.equals(row.getStatus())) {
            throw unauthorized("CDP write key is disabled");
        }
        return new AuthenticatedWriteKey(
                row.getId(),
                row.getTenantId(),
                row.getKeyPrefix(),
                row.getPlatform(),
                row.getRateLimitQps(),
                row.getDailyQuota());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 generate raw key 生成的文本或业务键。
     */
    public String generateRawKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "ck_" + HexFormat.of().formatHex(bytes);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param createdBy created by 参数，用于 create 流程中的校验、计算或对象转换。
     * @param rawKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public CdpWriteKeyDO create(Long tenantId, CdpWriteKeyCreateReq req, String createdBy, String rawKey) {
        String name = requireText(req.name(), "name");
        CdpWriteKeyDO row = new CdpWriteKeyDO();
        row.setTenantId(tenantId == null ? 0L : tenantId);
        row.setName(name);
        row.setKeyPrefix(prefix(rawKey));
        row.setKeyHash(passwordEncoder.encode(rawKey));
        row.setPlatform(normalizePlatform(req.platform()));
        row.setStatus(CdpWriteKeyDO.ACTIVE);
        row.setRateLimitQps(req.rateLimitQps() == null || req.rateLimitQps() <= 0 ? 100 : req.rateLimitQps());
        row.setDailyQuota(req.dailyQuota());
        row.setDescription(req.description());
        row.setCreatedBy(createdBy);
        writeKeyMapper.insert(row);
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<CdpWriteKeyDO> listTenantKeys(Long tenantId) {
        return writeKeyMapper.selectList(new LambdaQueryWrapper<CdpWriteKeyDO>()
                .eq(CdpWriteKeyDO::getTenantId, tenantId == null ? 0L : tenantId)
                .orderByDesc(CdpWriteKeyDO::getId));
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     */
    public void disable(Long tenantId, Long id) {
        CdpWriteKeyDO row = writeKeyMapper.selectById(id);
        Long normalizedTenantId = tenantId == null ? 0L : tenantId;
        if (row == null || !normalizedTenantId.equals(row.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CDP write key not found");
        }
        row.setStatus(CdpWriteKeyDO.DISABLED);
        writeKeyMapper.updateById(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param raw raw 参数，用于 prefix 流程中的校验、计算或对象转换。
     * @return 返回 prefix 生成的文本或业务键。
     */
    public String prefix(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("write key cannot be blank");
        }
        return raw.substring(0, Math.min(raw.length(), PREFIX_LENGTH));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 extract basic write key 生成的文本或业务键。
     */
    private String extractBasicWriteKey(HttpHeaders headers) {
        // 准备本次处理所需的上下文和中间变量。
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (auth == null || !auth.startsWith("Basic ")) {
            throw unauthorized("CDP write key is required");
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            String raw = colon >= 0 ? decoded.substring(0, colon) : decoded;
            if (raw.isBlank()) {
                throw unauthorized("CDP write key is required");
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return raw;
        } catch (IllegalArgumentException e) {
            throw unauthorized("CDP write key is malformed");
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param platform platform 参数，用于 normalizePlatform 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "WEB";
        }
        return platform.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 unauthorized 流程生成的业务结果。
     */
    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
