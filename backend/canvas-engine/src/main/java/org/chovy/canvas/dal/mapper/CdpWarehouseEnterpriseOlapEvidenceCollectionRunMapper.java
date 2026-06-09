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

/**
 * CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper 定义 dal.mapper 场景中的扩展契约。
 */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 insert 计算得到的数量、金额或指标值。
     */
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
    /**
     * 执行数据写入或状态变更。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param finishedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param evidenceCount evidence count 参数，用于 updateFinished 流程中的校验、计算或对象转换。
     * @param passCount pass count 参数，用于 updateFinished 流程中的校验、计算或对象转换。
     * @param warnCount warn count 参数，用于 updateFinished 流程中的校验、计算或对象转换。
     * @param failCount fail count 参数，用于 updateFinished 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回流程执行后的业务结果。
     */
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
    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    List<CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO> listRecent(@Param("tenantId") Long tenantId,
                                                                       @Param("limit") int limit);
}
