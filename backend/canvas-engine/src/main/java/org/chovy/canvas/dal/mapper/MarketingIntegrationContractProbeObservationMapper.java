package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeObservationDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeWindowStatsDO;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractProbeObservationMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param contractId 业务对象 ID，用于定位具体记录。
     * @param probeKey 业务键，用于在同一租户下定位资源。
     * @param windowStart window start 参数，用于 selectWindowStats 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    MarketingIntegrationContractProbeWindowStatsDO selectWindowStats(
            @Param("tenantId") Long tenantId,
            @Param("contractId") Long contractId,
            @Param("probeKey") String probeKey,
            @Param("windowStart") LocalDateTime windowStart);
}
