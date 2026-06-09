package org.chovy.canvas.domain.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.stereotype.Repository;

/**
 * MyBatisAudienceDefinitionRepository 编排 domain.analytics 场景的领域业务规则。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAudienceDefinitionRepository implements AudienceMaterializationService.AudienceDefinitionRepository {

    private final AudienceDefinitionMapper mapper;

    /**
     * requireEnabled 处理 domain.analytics 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireEnabled 流程生成的业务结果。
     */
    @Override
    public AudienceDefinitionDO requireEnabled(Long tenantId, Long audienceId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        if (audienceId == null) {
            throw new IllegalArgumentException("audienceId is required");
        }
        return mapper.selectOne(new LambdaQueryWrapper<AudienceDefinitionDO>()
                .eq(AudienceDefinitionDO::getTenantId, scopedTenantId)
                .eq(AudienceDefinitionDO::getId, audienceId)
                .eq(AudienceDefinitionDO::getEnabled, 1)
                .last("LIMIT 1"));
    }
}
