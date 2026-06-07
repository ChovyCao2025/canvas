package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;

import java.time.LocalDateTime;

@Mapper
public interface MarketingIntegrationContractProbeRunMapper
        extends BaseMapper<MarketingIntegrationContractProbeRunDO> {

    @Select("""
            SELECT COUNT(DISTINCT probe.contract_id)
            FROM marketing_integration_contract_probe_run probe
            INNER JOIN marketing_integration_contract contract
              ON contract.id = probe.contract_id
             AND contract.tenant_id = probe.tenant_id
            WHERE probe.tenant_id = #{tenantId}
              AND contract.environment = 'PRODUCTION'
              AND contract.status = 'ACTIVE'
              AND probe.environment = 'PRODUCTION'
              AND probe.status = 'PASS'
              AND probe.observed_at >= #{observedAfter}
            """)
    Long countFreshPassingProductionContracts(@Param("tenantId") Long tenantId,
                                              @Param("observedAfter") LocalDateTime observedAfter);

    @Select("""
            SELECT COUNT(DISTINCT probe.contract_id)
            FROM marketing_integration_contract_probe_run probe
            INNER JOIN marketing_integration_contract contract
              ON contract.id = probe.contract_id
             AND contract.tenant_id = probe.tenant_id
            WHERE probe.tenant_id = #{tenantId}
              AND contract.environment = 'PRODUCTION'
              AND contract.status = 'ACTIVE'
              AND probe.environment = 'PRODUCTION'
              AND probe.status IN ('WARN', 'FAIL')
              AND probe.observed_at >= #{observedAfter}
            """)
    Long countFreshFailingProductionContracts(@Param("tenantId") Long tenantId,
                                              @Param("observedAfter") LocalDateTime observedAfter);
}
