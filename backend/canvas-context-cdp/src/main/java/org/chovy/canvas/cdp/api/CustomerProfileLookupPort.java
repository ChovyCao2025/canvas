package org.chovy.canvas.cdp.api;

/**
 * 定义 CustomerProfileLookupPort 的协作契约。
 */
public interface CustomerProfileLookupPort {

    /**
     * 执行 ensureUser 对应的 CDP 业务操作。
     */
    CdpCustomerProfileView ensureUser(Long tenantId, String userId, String sourceType, String sourceRefId);

    /**
     * 执行 ensureUserByIdentity 对应的 CDP 业务操作。
     */
    CdpCustomerProfileView ensureUserByIdentity(
            Long tenantId,
            String identityType,
            String identityValue,
            String sourceType,
            /**
             * source Ref Id)。
             */
            String sourceRefId);

    /**
     * 返回required Profile。
     */
    CdpCustomerProfileView getRequiredProfile(Long tenantId, String userId);
}
