package org.chovy.canvas.cdp.domain;

public interface CustomerProfileRepository {

    CustomerProfile findProfile(Long tenantId, String userId);

    CustomerProfile saveProfile(CustomerProfile profile);

    String findUserIdByIdentity(Long tenantId, String identityType, String identityValue);

    void saveIdentity(Long tenantId, String userId, String identityType, String identityValue,
                      String sourceType, String sourceRefId, boolean verified);
}
