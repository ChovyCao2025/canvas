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

@Service
@RequiredArgsConstructor
public class CdpUserService {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;

    public CdpUserProfileDO ensureUser(String userId, String sourceType, String sourceRefId) {
        String normalized = requireUserId(userId);
        CdpUserProfileDO existing = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfileDO>().eq(CdpUserProfileDO::getUserId, normalized));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (existing.getFirstSeenAt() == null) {
                existing.setFirstSeenAt(now);
            }
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
            return ensureUser(existingIdentity.getUserId(), sourceType, sourceRefId);
        }

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

    public CdpUserProfileDO getRequiredProfile(String userId) {
        CdpUserProfileDO profile = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfileDO>().eq(CdpUserProfileDO::getUserId, requireUserId(userId)));
        if (profile == null) {
            throw new IllegalArgumentException("CDP用户不存在: " + userId);
        }
        return profile;
    }

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

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        return userId.trim();
    }

    private String normalizeIdentityType(String identityType) {
        if (identityType == null || identityType.isBlank()) {
            throw new IllegalArgumentException("identityType不能为空");
        }
        return identityType.trim().toUpperCase(Locale.ROOT);
    }

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
