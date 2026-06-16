package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 定义MybatisMarketingCampaignRepository的数据访问边界。
 */
@Repository
public class MybatisMarketingCampaignRepository implements MarketingCampaignRepository {

    /**
     * 营销活动主表的 MyBatis Mapper。
     */
    private final MarketingCampaignMasterMapper campaignMapper;

    /**
     * 营销活动资源关联表的 MyBatis Mapper。
     */
    private final MarketingCampaignLinkMapper linkMapper;

    /**
     * 领域对象与持久化对象之间的转换器。
     */
    private final MarketingCampaignPersistenceConverter converter;

    /**
     * 创建MybatisMarketingCampaignRepository实例。
     */
    public MybatisMarketingCampaignRepository(MarketingCampaignMasterMapper campaignMapper,
                                              MarketingCampaignLinkMapper linkMapper,
                                              MarketingCampaignPersistenceConverter converter) {
        this.campaignMapper = campaignMapper;
        this.linkMapper = linkMapper;
        this.converter = converter;
    }

    /**
     * 查找byTenantAndKey业务对象。
     */
    @Override
    public MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey) {
        MarketingCampaignMasterDO row = campaignMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                .eq(MarketingCampaignMasterDO::getCampaignKey, campaignKey.value())
                .last("LIMIT 1"));
        return converter.toCampaign(row);
    }

    /**
     * 查找byId业务对象。
     */
    @Override
    public MarketingCampaign findById(Long tenantId, Long campaignId) {
        MarketingCampaignMasterDO row = campaignMapper.selectById(campaignId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toCampaign(row);
    }

    /**
     * 执行save业务操作。
     */
    @Override
    public MarketingCampaign save(MarketingCampaign campaign) {
        MarketingCampaignMasterDO row = converter.toCampaignRow(campaign);
        if (row.getId() == null) {
            campaignMapper.insert(row);
        } else {
            campaignMapper.updateById(row);
        }
        return converter.toCampaign(row);
    }

    /**
     * 查询列表。
     */
    @Override
    public List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit) {
        String statusValue = status == null ? null : status.name();
        return campaignMapper.selectList(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                        .eq(statusValue != null, MarketingCampaignMasterDO::getStatus, statusValue)
                        .orderByDesc(MarketingCampaignMasterDO::getUpdatedAt)
                        .last("LIMIT " + limit))
                .stream()
                .filter(row -> statusValue == null || statusValue.equals(row.getStatus()))
                .map(converter::toCampaign)
                .toList();
    }

    /**
     * 查找link业务对象。
     */
    @Override
    public MarketingCampaignLink findLink(Long tenantId, Long campaignId, String resourceType, CampaignKey resourceKey) {
        MarketingCampaignLinkDO row = linkMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                .eq(MarketingCampaignLinkDO::getTenantId, tenantId)
                .eq(MarketingCampaignLinkDO::getCampaignId, campaignId)
                .eq(MarketingCampaignLinkDO::getResourceType, resourceType)
                .eq(MarketingCampaignLinkDO::getResourceKey, resourceKey.value())
                .last("LIMIT 1"));
        return converter.toLink(row);
    }

    /**
     * 执行saveLink业务操作。
     */
    @Override
    public MarketingCampaignLink saveLink(MarketingCampaignLink link) {
        MarketingCampaignLinkDO row = converter.toLinkRow(link);
        if (row.getId() == null) {
            linkMapper.insert(row);
        } else {
            linkMapper.updateById(row);
        }
        return converter.toLink(row);
    }

    /**
     * 查询links列表。
     */
    @Override
    public List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId) {
        return linkMapper.selectList(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, tenantId)
                        .eq(MarketingCampaignLinkDO::getCampaignId, campaignId)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceType)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceKey))
                .stream()
                .map(converter::toLink)
                .toList();
    }

    /**
     * 查找linkById业务对象。
     */
    @Override
    public MarketingCampaignLink findLinkById(Long tenantId, Long linkId) {
        MarketingCampaignLinkDO row = linkMapper.selectById(linkId);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            return null;
        }
        return converter.toLink(row);
    }

    /**
     * 删除或停用link业务对象。
     */
    @Override
    public void deleteLink(Long tenantId, Long linkId) {
        linkMapper.deleteById(linkId);
    }
}
