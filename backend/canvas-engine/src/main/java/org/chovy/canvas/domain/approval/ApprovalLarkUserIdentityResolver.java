package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ApprovalLarkUserIdentityDO;
import org.chovy.canvas.dal.mapper.ApprovalLarkUserIdentityMapper;
import org.springframework.stereotype.Service;

/**
 * ApprovalLarkUserIdentityResolver 编排 domain.approval 场景的领域业务规则。
 */
@Service
public class ApprovalLarkUserIdentityResolver {

    private final ApprovalLarkUserIdentityMapper mapper;

    /**
     * 创建 ApprovalLarkUserIdentityResolver 实例并注入 domain.approval 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ApprovalLarkUserIdentityResolver(ApprovalLarkUserIdentityMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户和本地用户名查找审批人对应的飞书身份。
     *
     * <p>该方法只读取身份映射表，不创建或同步飞书用户。用户名会先去除首尾空白；当租户、用户名、Mapper
     * 不可用，或记录中 openId、userId、departmentId 均为空时返回 {@code null}。</p>
     *
     * @param tenantId 本地租户 ID，用于隔离不同租户的飞书身份映射
     * @param username 审批任务中的本地用户名
     * @return 可用于创建或匹配飞书审批任务的身份信息；未配置时返回 {@code null}
     */
    public ApprovalLarkUserIdentity resolve(Long tenantId, String username) {
        String user = trimToNull(username);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (mapper == null || tenantId == null || user == null) {
            return null;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ApprovalLarkUserIdentity(openId, userId, departmentId);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 trimToNull 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }
}
