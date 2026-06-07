package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ApprovalLarkUserIdentityDO;
import org.chovy.canvas.dal.mapper.ApprovalLarkUserIdentityMapper;
import org.springframework.stereotype.Service;

@Service
public class ApprovalLarkUserIdentityResolver {

    private final ApprovalLarkUserIdentityMapper mapper;

    public ApprovalLarkUserIdentityResolver(ApprovalLarkUserIdentityMapper mapper) {
        this.mapper = mapper;
    }

    public ApprovalLarkUserIdentity resolve(Long tenantId, String username) {
        String user = trimToNull(username);
        if (mapper == null || tenantId == null || user == null) {
            return null;
        }
        ApprovalLarkUserIdentityDO row = mapper.selectOne(new LambdaQueryWrapper<ApprovalLarkUserIdentityDO>()
                .eq(ApprovalLarkUserIdentityDO::getTenantId, tenantId)
                .eq(ApprovalLarkUserIdentityDO::getUsername, user)
                .last("LIMIT 1"));
        if (row == null) {
            return null;
        }
        String openId = trimToNull(row.getLarkOpenId());
        String userId = trimToNull(row.getLarkUserId());
        String departmentId = trimToNull(row.getLarkDepartmentId());
        if (openId == null && userId == null && departmentId == null) {
            return null;
        }
        return new ApprovalLarkUserIdentity(openId, userId, departmentId);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }
}
