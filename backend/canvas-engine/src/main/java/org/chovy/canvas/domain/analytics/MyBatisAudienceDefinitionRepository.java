package org.chovy.canvas.domain.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisAudienceDefinitionRepository implements AudienceMaterializationService.AudienceDefinitionRepository {

    private final AudienceDefinitionMapper mapper;

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
