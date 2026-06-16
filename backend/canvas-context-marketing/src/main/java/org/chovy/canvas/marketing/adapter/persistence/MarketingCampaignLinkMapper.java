package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 定义MarketingCampaignLinkMapper的营销上下文访问契约。
 */
@Mapper
public interface MarketingCampaignLinkMapper extends BaseMapper<MarketingCampaignLinkDO> {

            /**
             * 执行COUNT业务操作。
             */
    @Select("""
            SELECT COUNT(DISTINCT c.id)
            FROM marketing_campaign_master c
            JOIN marketing_campaign_link l
              ON l.tenant_id = c.tenant_id
             AND l.campaign_id = c.id
            WHERE c.tenant_id = #{tenantId}
              AND c.status = 'ACTIVE'
              AND l.required_for_launch = 1
              AND l.link_status <> 'ACTIVE'
            """)
    /**
     * 执行countActiveCampaignsWithInactiveRequiredLinks业务操作。
     */
    Long countActiveCampaignsWithInactiveRequiredLinks(@Param("tenantId") Long tenantId);

            /**
             * 执行COUNT业务操作。
             */
    @Select("""
            SELECT COUNT(*)
            FROM marketing_campaign_master c
            WHERE c.tenant_id = #{tenantId}
              AND c.status = 'ACTIVE'
              /**
               * 执行EXISTS业务操作。
               */
              AND NOT EXISTS (
                SELECT 1
                FROM marketing_campaign_link l
                WHERE l.tenant_id = c.tenant_id
                  AND l.campaign_id = c.id
                  AND l.required_for_launch = 1
                  AND l.link_status = 'ACTIVE'
                  AND l.dependency_role = 'PRIMARY'
              )
            """)
    /**
     * 执行countActiveCampaignsMissingPrimaryDependency业务操作。
     */
    Long countActiveCampaignsMissingPrimaryDependency(@Param("tenantId") Long tenantId);

            /**
             * 执行COUNT业务操作。
             */
    @Select("""
            SELECT COUNT(*)
            FROM marketing_campaign_master c
            WHERE c.tenant_id = #{tenantId}
              AND c.status = 'ACTIVE'
              /**
               * 执行EXISTS业务操作。
               */
              AND NOT EXISTS (
                SELECT 1
                FROM marketing_campaign_link l
                WHERE l.tenant_id = c.tenant_id
                  AND l.campaign_id = c.id
                  AND l.required_for_launch = 1
                  AND l.link_status = 'ACTIVE'
                  /**
                   * 执行AND业务操作。
                   */
                  AND (l.dependency_role = 'MEASUREMENT' OR l.resource_type = 'BI_DASHBOARD')
              )
            """)
    /**
     * 执行countActiveCampaignsMissingMeasurementDependency业务操作。
     */
    Long countActiveCampaignsMissingMeasurementDependency(@Param("tenantId") Long tenantId);
}
