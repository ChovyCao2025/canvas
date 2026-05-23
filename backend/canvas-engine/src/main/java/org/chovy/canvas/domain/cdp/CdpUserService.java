package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CdpUserService {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;

    public CdpUserProfile ensureUser(String userId, String sourceType, String sourceRefId) {
        String normalized = requireUserId(userId);
        CdpUserProfile existing = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfile>().eq(CdpUserProfile::getUserId, normalized));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (existing.getFirstSeenAt() == null) {
                existing.setFirstSeenAt(now);
            }
            existing.setLastSeenAt(now);
            profileMapper.updateById(existing);
            return existing;
        }

        CdpUserProfile created = new CdpUserProfile();
        created.setUserId(normalized);
        created.setDisplayName(normalized);
        created.setStatus("ACTIVE");
        created.setFirstSeenAt(now);
        created.setLastSeenAt(now);
        profileMapper.insert(created);

        CdpUserIdentity identity = new CdpUserIdentity();
        identity.setUserId(normalized);
        identity.setIdentityType("USER_ID");
        identity.setIdentityValue(normalized);
        identity.setSourceType(sourceType);
        identity.setSourceRefId(sourceRefId);
        identity.setVerified(1);
        identityMapper.insert(identity);

        return created;
    }

    public CdpUserProfile getRequiredProfile(String userId) {
        CdpUserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<CdpUserProfile>().eq(CdpUserProfile::getUserId, requireUserId(userId)));
        if (profile == null) {
            throw new IllegalArgumentException("CDP用户不存在: " + userId);
        }
        return profile;
    }

    public CdpUserDetailDTO toDetail(CdpUserProfile profile) {
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
