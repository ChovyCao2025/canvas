package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;

import java.time.LocalDateTime;

@Mapper
public interface MarketingIntegrationContractProbeObservationMapper
        extends BaseMapper<MarketingIntegrationContractProbeObservationDO> {

    @Select("""
            SELECT
              COUNT(1) AS total_count,
              SUM(CASE WHEN status <> 'PASS' THEN 1 ELSE 0 END) AS bad_count
            FROM marketing_integration_contract_probe_observation
            WHERE tenant_id = #{tenantId}
              AND contract_id = #{contractId}
              AND probe_key = #{probeKey}
              AND observed_at >= #{windowStart}
            """)
    MarketingIntegrationContractProbeWindowStatsDO selectWindowStats(
            @Param("tenantId") Long tenantId,
            @Param("contractId") Long contractId,
            @Param("probeKey") String probeKey,
            @Param("windowStart") LocalDateTime windowStart);
}
