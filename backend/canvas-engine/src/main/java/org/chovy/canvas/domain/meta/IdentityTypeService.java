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

/**
 * 身份类型 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class IdentityTypeService {

    /** 身份类型编码格式：小写字母开头，后续允许小写字母、数字和下划线。 */
    private static final Pattern CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]{1,63}");

    /** 身份类型 Mapper。 */
    private final IdentityTypeMapper identityTypeMapper;
    /** CDP 用户身份 Mapper。 */
    private final CdpUserIdentityMapper cdpUserIdentityMapper;

    /** 按条件查询列表数据。 */
    public List<IdentityTypeDO> list(Integer enabled, Integer allowImport) {
        return identityTypeMapper.selectList(new LambdaQueryWrapper<IdentityTypeDO>()
                .eq(enabled != null, IdentityTypeDO::getEnabled, enabled)
                .eq(allowImport != null, IdentityTypeDO::getAllowImport, allowImport)
                .orderByAsc(IdentityTypeDO::getId));
    }

    /** 查询启用且允许导入的身份类型列表。 */
    public List<IdentityTypeDO> listImportable() {
        return list(1, 1);
    }

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public IdentityTypeDO create(IdentityTypeDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        identityTypeMapper.insert(body);
        return body;
    }

    /** 更新已有记录，仅修改允许变更的字段。 */
    public void update(Long id, IdentityTypeDO body) {
        validateAndNormalize(body);
        applyDefaults(body);
        body.setId(id);
        identityTypeMapper.updateById(body);
    }

    /** 删除身份类型，若已被 CDP 用户身份引用则阻止删除。 */
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

    /** 查询可导入的身份类型配置，不存在、禁用或不允许导入时抛出异常。 */
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

    /** 校验身份类型必填字段并规范化编码、名称。 */
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

    /** 为身份类型写入默认启用、可导入、优先级等配置。 */
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

    /** 将后台配置编码统一转换为小写形式。 */
    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    /** 将后台身份类型编码转换为 CDP 身份表使用的大写编码。 */
    private static String toCdpIdentityType(String code) {
        return normalizeCode(code).toUpperCase(Locale.ROOT);
    }
}
