package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.mapper.IdentityTypeMapper;

@Service
@RequiredArgsConstructor
public class IdentityTypeService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]{1,63}");

    private final IdentityTypeMapper identityTypeMapper;
    private final CdpUserIdentityMapper cdpUserIdentityMapper;

    public List<IdentityTypeDO> list(Integer enabled, Integer allowImport) {
        return identityTypeMapper.selectList(new LambdaQueryWrapper<IdentityTypeDO>()
                .eq(enabled != null, IdentityTypeDO::getEnabled, enabled)
                .eq(allowImport != null, IdentityTypeDO::getAllowImport, allowImport)
                .orderByAsc(IdentityTypeDO::getId));
    }

    public List<IdentityTypeDO> listImportable() {
        return list(1, 1);
    }

    public IdentityTypeDO create(IdentityTypeDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        identityTypeMapper.insert(body);
        return body;
    }

    public void update(Long id, IdentityTypeDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        body.setId(id);
        identityTypeMapper.updateById(body);
    }

    public void delete(Long id) {
        IdentityTypeDO existing = identityTypeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("identity type not found: " + id);
        }
        Long count = cdpUserIdentityMapper.selectCount(new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getIdentityType, toCdpIdentityType(existing.getCode())));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("identity type is in use: " + existing.getCode());
        }
        identityTypeMapper.deleteById(id);
    }

    public IdentityTypeDO requireImportable(String code) {
        String normalizedCode = normalizeCode(code);
        IdentityTypeDO identityType = identityTypeMapper.selectOne(new LambdaQueryWrapper<IdentityTypeDO>()
                .eq(IdentityTypeDO::getCode, normalizedCode));
        if (identityType == null || identityType.getEnabled() == null || identityType.getEnabled() != 1
                || identityType.getAllowImport() == null || identityType.getAllowImport() != 1) {
            throw new IllegalArgumentException("identity type is not importable: " + normalizedCode);
        }
        return identityType;
    }

    private static void validateAndNormalize(IdentityTypeDO body) {
        if (body == null) {
            throw new IllegalArgumentException("identity type body is required");
        }
        String normalizedCode = normalizeCode(body.getCode());
        if (normalizedCode == null || normalizedCode.isEmpty()) {
            throw new IllegalArgumentException("code is required");
        }
        if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException("invalid code: " + normalizedCode);
        }
        if (body.getName() == null || body.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        body.setCode(normalizedCode);
        body.setName(body.getName().trim());
    }

    private static void applyDefaults(IdentityTypeDO body) {
        if (body.getEnabled() == null) {
            body.setEnabled(1);
        }
        if (body.getAllowImport() == null) {
            body.setAllowImport(1);
        }
        if (body.getMultiValue() == null) {
            body.setMultiValue(0);
        }
        if (body.getPriority() == null) {
            body.setPriority(100);
        }
        if (body.getParticipateMapping() == null) {
            body.setParticipateMapping(0);
        }
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private static String toCdpIdentityType(String code) {
        return normalizeCode(code).toUpperCase(Locale.ROOT);
    }
}
