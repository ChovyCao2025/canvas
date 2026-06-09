package org.chovy.canvas.dal.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceDO;

import java.util.List;

/**
 * CdpWarehouseEnterpriseOlapEvidenceMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface CdpWarehouseEnterpriseOlapEvidenceMapper {

    @Insert("""
            INSERT INTO cdp_warehouse_enterprise_olap_evidence
                (tenant_id, evidence_key, source, status, reason, measured_at, expires_at, evidence_json, created_by)
            VALUES
                (#{row.tenantId}, #{row.evidenceKey}, #{row.source}, #{row.status}, #{row.reason},
                 #{row.measuredAt}, #{row.expiresAt}, #{row.evidenceJson}, #{row.createdBy})
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 insert 计算得到的数量、金额或指标值。
     */
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insert(@Param("row") CdpWarehouseEnterpriseOlapEvidenceDO row);

    @Select("""
            SELECT *
            FROM cdp_warehouse_enterprise_olap_evidence
            WHERE tenant_id = #{tenantId}
            ORDER BY measured_at DESC, id DESC
            LIMIT #{limit}
            """)
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseEnterpriseOlapEvidenceDO> listRecent(@Param("tenantId") Long tenantId,
                                                          @Param("limit") int limit);
}
