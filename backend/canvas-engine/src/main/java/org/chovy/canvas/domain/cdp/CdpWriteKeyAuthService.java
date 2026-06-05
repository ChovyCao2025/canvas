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
public class CdpWriteKeyAuthService {
    private static final int PREFIX_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CdpWriteKeyMapper writeKeyMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public record AuthenticatedWriteKey(
            Long writeKeyId,
            Long tenantId,
            String keyPrefix,
            String platform,
            Integer rateLimitQps,
            Long dailyQuota
    ) {
    }

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

    public String generateRawKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "ck_" + HexFormat.of().formatHex(bytes);
    }

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

    public List<CdpWriteKeyDO> listTenantKeys(Long tenantId) {
        return writeKeyMapper.selectList(new LambdaQueryWrapper<CdpWriteKeyDO>()
                .eq(CdpWriteKeyDO::getTenantId, tenantId == null ? 0L : tenantId)
                .orderByDesc(CdpWriteKeyDO::getId));
    }

    public void disable(Long tenantId, Long id) {
        CdpWriteKeyDO row = writeKeyMapper.selectById(id);
        Long normalizedTenantId = tenantId == null ? 0L : tenantId;
        if (row == null || !normalizedTenantId.equals(row.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CDP write key not found");
        }
        row.setStatus(CdpWriteKeyDO.DISABLED);
        writeKeyMapper.updateById(row);
    }

    public String prefix(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("write key cannot be blank");
        }
        return raw.substring(0, Math.min(raw.length(), PREFIX_LENGTH));
    }

    private String extractBasicWriteKey(HttpHeaders headers) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
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
            return raw;
        } catch (IllegalArgumentException e) {
            throw unauthorized("CDP write key is malformed");
        }
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "WEB";
        }
        return platform.trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
