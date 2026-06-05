package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.compliance.PiiMaskingService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
import org.springframework.beans.factory.ObjectProvider;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;

/**
 * CDP 用户 CDP 领域服务。
 *
 * <p>负责用户画像、身份、标签和画布参与记录等客户数据能力，为画布执行和管理端查询提供统一入口。
 * <p>该层隔离 CDP 数据结构与上层业务，集中处理状态、历史和幂等语义。
 */
@Service
public class CdpUserService {

    /** CDP 用户画像 Mapper，用于维护用户主档和最近出现时间。 */
    private final CdpUserProfileMapper profileMapper;
    /** 用户身份 Mapper。 */
    private final CdpUserIdentityMapper identityMapper;
    /** 集中 PII 脱敏服务。 */
    private final PiiMaskingService maskingService;
    private final ObjectProvider<CdpWarehousePrivacyTombstoneService> privacyTombstoneService;
    /** JSON 转换器，用于属性 JSON 脱敏。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CdpUserService(CdpUserProfileMapper profileMapper, CdpUserIdentityMapper identityMapper) {
        this(profileMapper, identityMapper, new PiiMaskingService(), null);
    }

    public CdpUserService(CdpUserProfileMapper profileMapper,
                          CdpUserIdentityMapper identityMapper,
                          PiiMaskingService maskingService) {
        this(profileMapper, identityMapper, maskingService, null);
    }

    @Autowired
    public CdpUserService(CdpUserProfileMapper profileMapper,
                          CdpUserIdentityMapper identityMapper,
                          PiiMaskingService maskingService,
                          ObjectProvider<CdpWarehousePrivacyTombstoneService> privacyTombstoneService) {
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.maskingService = maskingService;
        this.privacyTombstoneService = privacyTombstoneService;
    }

    /** 确保 CDP 用户画像存在，不存在时按来源创建。 */
    public CdpUserProfileDO ensureUser(String userId, String sourceType, String sourceRefId) {
        return ensureUser(null, userId, sourceType, sourceRefId);
    }

    /** 确保指定租户内的 CDP 用户画像存在，不存在时按来源创建。 */
    public CdpUserProfileDO ensureUser(Long tenantId, String userId, String sourceType, String sourceRefId) {
        String normalized = requireUserId(userId);
        enforcePrivacyTombstone(tenantId, "USER_ID", normalized, "CDP_USER_ENSURE");
        CdpUserProfileDO existing = profileMapper.selectOne(profileQuery(tenantId)
                .eq(CdpUserProfileDO::getUserId, normalized));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (existing.getFirstSeenAt() == null) {
                existing.setFirstSeenAt(now);
            }
            // 已存在用户只刷新最近出现时间，避免覆盖管理端维护的画像字段。
            existing.setLastSeenAt(now);
            profileMapper.updateById(existing);
            return existing;
        }

        CdpUserProfileDO created = new CdpUserProfileDO();
        created.setTenantId(tenantId);
        created.setUserId(normalized);
        created.setDisplayName(normalized);
        created.setStatus("ACTIVE");
        created.setFirstSeenAt(now);
        created.setLastSeenAt(now);
        profileMapper.insert(created);

        CdpUserIdentityDO identity = new CdpUserIdentityDO();
        identity.setTenantId(tenantId);
        identity.setUserId(normalized);
        identity.setIdentityType("USER_ID");
        identity.setIdentityValue(normalized);
        identity.setSourceType(sourceType);
        identity.setSourceRefId(sourceRefId);
        identity.setVerified(1);
        identityMapper.insert(identity);

        return created;
    }

    /** 按身份类型和值确保用户存在，并补齐身份映射。 */
    public CdpUserProfileDO ensureUserByIdentity(String identityType, String identityValue, String sourceType, String sourceRefId) {
        return ensureUserByIdentity(null, identityType, identityValue, sourceType, sourceRefId);
    }

    /** 按租户、身份类型和值确保用户存在，并补齐身份映射。 */
    public CdpUserProfileDO ensureUserByIdentity(
            Long tenantId, String identityType, String identityValue, String sourceType, String sourceRefId) {
        String normalizedType = normalizeIdentityType(identityType);
        String normalizedValue = requireUserId(identityValue);
        if ("USER_ID".equals(normalizedType)) {
            return ensureUser(tenantId, normalizedValue, sourceType, sourceRefId);
        }
        enforcePrivacyTombstone(tenantId, normalizedType, normalizedValue, "CDP_USER_ENSURE_IDENTITY");

        CdpUserIdentityDO existingIdentity = identityMapper.selectOne(identityQuery(tenantId)
                .eq(CdpUserIdentityDO::getIdentityType, normalizedType)
                .eq(CdpUserIdentityDO::getIdentityValue, normalizedValue)
                .last("LIMIT 1"));
        if (existingIdentity != null) {
            // 外部身份已绑定时统一落到既有 userId，避免同一客户被拆成多个 CDP 用户。
            return ensureUser(tenantId, existingIdentity.getUserId(), sourceType, sourceRefId);
        }

        // 未绑定外部身份时生成稳定 userId，后续唯一身份记录会把相同身份收敛到同一用户。
        String generatedUserId = normalizedType.toLowerCase(Locale.ROOT) + ":" + normalizedValue;
        CdpUserProfileDO profile = ensureUser(tenantId, generatedUserId, sourceType, sourceRefId);
        CdpUserIdentityDO identity = new CdpUserIdentityDO();
        identity.setTenantId(tenantId);
        identity.setUserId(profile.getUserId());
        identity.setIdentityType(normalizedType);
        identity.setIdentityValue(normalizedValue);
        identity.setSourceType(sourceType);
        identity.setSourceRefId(sourceRefId);
        identity.setVerified(0);
        try {
            identityMapper.insert(identity);
        } catch (DuplicateKeyException duplicate) {
            // 并发导入同一外部身份时，以已成功插入的身份映射为准重新归并用户。
            CdpUserIdentityDO raced = identityMapper.selectOne(identityQuery(tenantId)
                    .eq(CdpUserIdentityDO::getIdentityType, normalizedType)
                    .eq(CdpUserIdentityDO::getIdentityValue, normalizedValue)
                    .last("LIMIT 1"));
            if (raced != null) {
                return ensureUser(tenantId, raced.getUserId(), sourceType, sourceRefId);
            }
            throw duplicate;
        }
        return profile;
    }

    /** 查询用户画像，不存在时抛出业务异常。 */
    public CdpUserProfileDO getRequiredProfile(String userId) {
        return getRequiredProfile(null, userId);
    }

    /** 查询指定租户内用户画像，不存在时抛出业务异常。 */
    public CdpUserProfileDO getRequiredProfile(Long tenantId, String userId) {
        CdpUserProfileDO profile = profileMapper.selectOne(profileQuery(tenantId)
                .eq(CdpUserProfileDO::getUserId, requireUserId(userId)));
        if (profile == null) {
            throw new IllegalArgumentException("CDP用户不存在: " + userId);
        }
        return profile;
    }

    /** 将用户画像实体转换为前端详情 DTO。 */
    public CdpUserDetailDTO toDetail(CdpUserProfileDO profile) {
        return new CdpUserDetailDTO(
                profile.getUserId(),
                profile.getDisplayName(),
                maskingService.maskPhone(profile.getPhone()),
                maskingService.maskEmail(profile.getEmail()),
                profile.getStatus(),
                maskProperties(profile.getPropertiesJson()),
                profile.getFirstSeenAt(),
                profile.getLastSeenAt()
        );
    }

    /** 校验用户标识并返回去除首尾空白后的值。 */
    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        return userId.trim();
    }

    /** 规范化外部身份类型，统一使用大写编码。 */
    private String normalizeIdentityType(String identityType) {
        if (identityType == null || identityType.isBlank()) {
            throw new IllegalArgumentException("identityType不能为空");
        }
        return identityType.trim().toUpperCase(Locale.ROOT);
    }

    private String maskProperties(String propertiesJson) {
        if (propertiesJson == null || propertiesJson.isBlank()) {
            return propertiesJson;
        }
        try {
            Map<String, Object> properties = objectMapper.readValue(
                    propertiesJson, new TypeReference<Map<String, Object>>() {});
            return objectMapper.writeValueAsString(maskingService.maskMetadata(properties));
        } catch (JsonProcessingException ignored) {
            return maskingService.maskText(propertiesJson);
        }
    }

    private LambdaQueryWrapper<CdpUserProfileDO> profileQuery(Long tenantId) {
        LambdaQueryWrapper<CdpUserProfileDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CdpUserProfileDO::getTenantId, tenantId);
        }
        return query;
    }

    private LambdaQueryWrapper<CdpUserIdentityDO> identityQuery(Long tenantId) {
        LambdaQueryWrapper<CdpUserIdentityDO> query = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            query.eq(CdpUserIdentityDO::getTenantId, tenantId);
        }
        return query;
    }

    private void enforcePrivacyTombstone(Long tenantId, String subjectType, String subjectValue, String source) {
        CdpWarehousePrivacyTombstoneService service =
                privacyTombstoneService == null ? null : privacyTombstoneService.getIfAvailable();
        if (service == null) {
            return;
        }
        service.enforceNotBlocked(tenantId, subjectType, subjectValue, source);
    }
}
