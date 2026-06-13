package org.chovy.canvas.cdp.api;

public interface CustomerProfileLookupPort {

    CdpCustomerProfileView ensureUser(Long tenantId, String userId, String sourceType, String sourceRefId);

    CdpCustomerProfileView ensureUserByIdentity(
            Long tenantId,
            String identityType,
            String identityValue,
            String sourceType,
            String sourceRefId);

    CdpCustomerProfileView getRequiredProfile(Long tenantId, String userId);
}
