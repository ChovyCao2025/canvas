package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCustomerProfileRepository implements CustomerProfileRepository {

    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final CdpPersistenceConverter converter;

    public MybatisCustomerProfileRepository(CdpUserProfileMapper profileMapper,
                                            CdpUserIdentityMapper identityMapper,
                                            CdpPersistenceConverter converter) {
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.converter = converter;
    }

    @Override
    public CustomerProfile findProfile(Long tenantId, String userId) {
        CdpUserProfileDO row = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getTenantId, tenantId)
                .eq(CdpUserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        return converter.toProfile(row);
    }

    @Override
    public CustomerProfile saveProfile(CustomerProfile profile) {
        CdpUserProfileDO row = converter.toProfileRow(profile);
        if (row.getId() == null) {
            profileMapper.insert(row);
        } else {
            profileMapper.updateById(row);
        }
        return converter.toProfile(row);
    }

    @Override
    public String findUserIdByIdentity(Long tenantId, String identityType, String identityValue) {
        CdpUserIdentityDO row = identityMapper.selectOne(new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getTenantId, tenantId)
                .eq(CdpUserIdentityDO::getIdentityType, identityType)
                .eq(CdpUserIdentityDO::getIdentityValue, identityValue)
                .last("LIMIT 1"));
        return row == null ? null : row.getUserId();
    }

    @Override
    public void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                             String sourceType, String sourceRefId, boolean verified) {
        try {
            identityMapper.insert(converter.toIdentityRow(tenantId, userId, identityType, identityValue,
                    sourceType, sourceRefId, verified));
        } catch (DuplicateKeyException ignored) {
            // A concurrent writer already established the same identity binding.
        }
    }
}
