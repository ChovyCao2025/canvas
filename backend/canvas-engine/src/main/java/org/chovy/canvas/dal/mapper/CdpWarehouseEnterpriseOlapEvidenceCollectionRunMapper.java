package org.chovy.canvas.dal.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.chovy.canvas.dal.dataobject.CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper {

    @Insert("""
            INSERT INTO cdp_warehouse_enterprise_olap_evidence_collection_run
                (tenant_id, trigger_type, status, started_at, evidence_count, pass_count, warn_count,
                 fail_count, reason, created_by)
            VALUES
                (#{row.tenantId}, #{row.triggerType}, #{row.status}, #{row.startedAt}, #{row.evidenceCount},
                 #{row.passCount}, #{row.warnCount}, #{row.failCount}, #{row.reason}, #{row.createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insert(@Param("row") CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO row);

    @Update("""
            UPDATE cdp_warehouse_enterprise_olap_evidence_collection_run
            SET status = #{status},
                finished_at = #{finishedAt},
                evidence_count = #{evidenceCount},
                pass_count = #{passCount},
                warn_count = #{warnCount},
                fail_count = #{failCount},
                reason = #{reason},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateFinished(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("finishedAt") LocalDateTime finishedAt,
                       @Param("evidenceCount") int evidenceCount,
                       @Param("passCount") int passCount,
                       @Param("warnCount") int warnCount,
                       @Param("failCount") int failCount,
                       @Param("reason") String reason);

    @Select("""
            SELECT *
            FROM cdp_warehouse_enterprise_olap_evidence_collection_run
            WHERE tenant_id = #{tenantId}
            ORDER BY started_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO> listRecent(@Param("tenantId") Long tenantId,
                                                                       @Param("limit") int limit);
}
