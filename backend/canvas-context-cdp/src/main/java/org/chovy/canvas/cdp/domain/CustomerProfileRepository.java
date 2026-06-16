package org.chovy.canvas.cdp.domain;

/**
 * 定义 CustomerProfile 的持久化访问契约。
 */
public interface CustomerProfileRepository {

    /**
     * 查找Profile。
     */
    CustomerProfile findProfile(Long tenantId, String userId);

    /**
     * 保存Profile。
     */
    CustomerProfile saveProfile(CustomerProfile profile);

    /**
     * 查找User Id By Identity。
     */
    String findUserIdByIdentity(Long tenantId, String identityType, String identityValue);

    /**
     * 保存Identity。
     */
    void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                      /**
                       * verified)。
                       */
                      String sourceType, String sourceRefId, boolean verified);
}
