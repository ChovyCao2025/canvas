package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CustomerProfile;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 定义 MybatisCustomerProfile 的持久化访问契约。
 */
@Repository
public class MybatisCustomerProfileRepository implements CustomerProfileRepository {

    /**
     * profile Mapper。
     */
    private final CdpUserProfileMapper profileMapper;

    /**
     * identity Mapper。
     */
    private final CdpUserIdentityMapper identityMapper;

    /**
     * 持久化转换器。
     */
    private final CdpPersistenceConverter converter;

    /**
     * 创建当前组件实例。
     */
    public MybatisCustomerProfileRepository(CdpUserProfileMapper profileMapper,
                                            CdpUserIdentityMapper identityMapper,
                                            CdpPersistenceConverter converter) {
        this.profileMapper = profileMapper;
        this.identityMapper = identityMapper;
        this.converter = converter;
    }

    /**
     * 查找Profile。
     */
    @Override
    public CustomerProfile findProfile(Long tenantId, String userId) {
        CdpUserProfileDO row = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                .eq(CdpUserProfileDO::getTenantId, tenantId)
                .eq(CdpUserProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        return converter.toProfile(row);
    }

    /**
     * 保存Profile。
     */
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

    /**
     * 查找User Id By Identity。
     */
    @Override
    public String findUserIdByIdentity(Long tenantId, String identityType, String identityValue) {
        CdpUserIdentityDO row = identityMapper.selectOne(new LambdaQueryWrapper<CdpUserIdentityDO>()
                .eq(CdpUserIdentityDO::getTenantId, tenantId)
                .eq(CdpUserIdentityDO::getIdentityType, identityType)
                .eq(CdpUserIdentityDO::getIdentityValue, identityValue)
                .last("LIMIT 1"));
        return row == null ? null : row.getUserId();
    }

    /**
     * 保存Identity。
     */
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
