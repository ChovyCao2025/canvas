package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO;

@Mapper
public interface MarketingCampaignLinkMapper extends BaseMapper<MarketingCampaignLinkDO> {

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
    Long countActiveCampaignsWithInactiveRequiredLinks(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM marketing_campaign_master c
            WHERE c.tenant_id = #{tenantId}
              AND c.status = 'ACTIVE'
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
    Long countActiveCampaignsMissingPrimaryDependency(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM marketing_campaign_master c
            WHERE c.tenant_id = #{tenantId}
              AND c.status = 'ACTIVE'
              AND NOT EXISTS (
                SELECT 1
                FROM marketing_campaign_link l
                WHERE l.tenant_id = c.tenant_id
                  AND l.campaign_id = c.id
                  AND l.required_for_launch = 1
                  AND l.link_status = 'ACTIVE'
                  AND (l.dependency_role = 'MEASUREMENT' OR l.resource_type = 'BI_DASHBOARD')
              )
            """)
    Long countActiveCampaignsMissingMeasurementDependency(@Param("tenantId") Long tenantId);
}
