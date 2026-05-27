package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
@RequiredArgsConstructor
public class CdpUserService {

    /** CDP 用户画像 Mapper，用于维护用户主档和最近出现时间。 */
    private final CdpUserProfileMapper profileMapper;
    /** 用户身份 Mapper。 */
    private final CdpUserIdentityMapper identityMapper;

    /** 确保 CDP 用户画像存在，不存在时按来源创建。 */
    public CdpUserProfileDO ensureUser(String userId, String sourceType, String sourceRefId) {
        String normalized = requireUserId(userId);
        CdpUserProfileDO existing = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfileDO>().eq(CdpUserProfileDO::getUserId, normalized));
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
        created.setUserId(normalized);
        created.setDisplayName(normalized);
        created.setStatus("ACTIVE");
        created.setFirstSeenAt(now);
        created.setLastSeenAt(now);
        profileMapper.insert(created);

        CdpUserIdentityDO identity = new CdpUserIdentityDO();
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
        String normalizedType = normalizeIdentityType(identityType);
        String normalizedValue = requireUserId(identityValue);
        if ("USER_ID".equals(normalizedType)) {
            return ensureUser(normalizedValue, sourceType, sourceRefId);
        }

        CdpUserIdentityDO existingIdentity = identityMapper.selectOne(new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getIdentityType, normalizedType)
                .eq(CdpUserIdentityDO::getIdentityValue, normalizedValue)
                .last("LIMIT 1"));
        if (existingIdentity != null) {
            // 外部身份已绑定时统一落到既有 userId，避免同一客户被拆成多个 CDP 用户。
            return ensureUser(existingIdentity.getUserId(), sourceType, sourceRefId);
        }

        // 未绑定外部身份时生成稳定 userId，后续唯一身份记录会把相同身份收敛到同一用户。
        String generatedUserId = normalizedType.toLowerCase(Locale.ROOT) + ":" + normalizedValue;
        CdpUserProfileDO profile = ensureUser(generatedUserId, sourceType, sourceRefId);
        CdpUserIdentityDO identity = new CdpUserIdentityDO();
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
            CdpUserIdentityDO raced = identityMapper.selectOne(new LambdaQueryWrapper<CdpUserIdentityDO>()
                    .eq(CdpUserIdentityDO::getIdentityType, normalizedType)
                    .eq(CdpUserIdentityDO::getIdentityValue, normalizedValue)
                    .last("LIMIT 1"));
            if (raced != null) {
                return ensureUser(raced.getUserId(), sourceType, sourceRefId);
            }
            throw duplicate;
        }
        return profile;
    }

    /** 查询用户画像，不存在时抛出业务异常。 */
    public CdpUserProfileDO getRequiredProfile(String userId) {
        CdpUserProfileDO profile = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfileDO>().eq(CdpUserProfileDO::getUserId, requireUserId(userId)));
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
                DataMaskingUtil.maskPhone(profile.getPhone()),
                maskEmail(profile.getEmail()),
                profile.getStatus(),
                profile.getPropertiesJson(),
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

    /** 对邮箱用户名部分做脱敏，保留首尾字符和域名。 */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        String name = email.substring(0, at);
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + email.substring(at);
    }
}
