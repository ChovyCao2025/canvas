package org.chovy.canvas.dal.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceDO;

import java.util.List;

@Mapper
public interface CdpWarehouseEnterpriseOlapEvidenceMapper {

    @Insert("""
            INSERT INTO cdp_warehouse_enterprise_olap_evidence
                (tenant_id, evidence_key, source, status, reason, measured_at, expires_at, evidence_json, created_by)
            VALUES
                (#{row.tenantId}, #{row.evidenceKey}, #{row.source}, #{row.status}, #{row.reason},
                 #{row.measuredAt}, #{row.expiresAt}, #{row.evidenceJson}, #{row.createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insert(@Param("row") CdpWarehouseEnterpriseOlapEvidenceDO row);

    @Select("""
            SELECT *
            FROM cdp_warehouse_enterprise_olap_evidence
            WHERE tenant_id = #{tenantId}
            ORDER BY measured_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CdpWarehouseEnterpriseOlapEvidenceDO> listRecent(@Param("tenantId") Long tenantId,
                                                          @Param("limit") int limit);
}
