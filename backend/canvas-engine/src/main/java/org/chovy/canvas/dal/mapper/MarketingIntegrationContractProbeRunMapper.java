package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractProbeRunDO;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractProbeRunMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface MarketingIntegrationContractProbeRunMapper
        extends BaseMapper<MarketingIntegrationContractProbeRunDO> {

            /**
             * 查询或读取业务数据。
             *
             * @param observedAfter observed after 参数，用于 COUNT 流程中的校验、计算或对象转换。
             * @return 返回 COUNT 流程生成的业务结果。
             */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param observedAfter observed after 参数，用于 countFreshPassingProductionContracts 流程中的校验、计算或对象转换。
     * @return 返回统计数量。
     */
    Long countFreshPassingProductionContracts(@Param("tenantId") Long tenantId,
                                              @Param("observedAfter") LocalDateTime observedAfter);

            /**
             * 查询或读取业务数据。
             *
             * @param observedAfter observed after 参数，用于 COUNT 流程中的校验、计算或对象转换。
             * @return 返回 COUNT 流程生成的业务结果。
             */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param observedAfter observed after 参数，用于 countFreshFailingProductionContracts 流程中的校验、计算或对象转换。
     * @return 返回统计数量。
     */
    Long countFreshFailingProductionContracts(@Param("tenantId") Long tenantId,
                                              @Param("observedAfter") LocalDateTime observedAfter);
}
