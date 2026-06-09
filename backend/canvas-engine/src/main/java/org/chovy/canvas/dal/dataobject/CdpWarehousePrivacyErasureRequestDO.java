package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehousePrivacyErasureRequestDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_privacy_erasure_request")
public class CdpWarehousePrivacyErasureRequestDO {

    /** CDP数仓隐私删除请求主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓隐私删除请求请求业务键 */
    private String requestKey;

    /** CDP数仓隐私删除请求主体类型 */
    private String subjectType;

    /** CDP数仓隐私删除请求主体哈希 */
    private String subjectHash;

    /** CDP数仓隐私删除请求主体引用脱敏 */
    private String subjectRefMasked;

    /** CDP数仓隐私删除请求原因说明 */
    private String reason;

    /** CDP数仓隐私删除请求请求人 */
    private String requestedBy;

    /** CDP数仓隐私删除请求当前状态 */
    private String status;

    /** CDP数仓隐私删除请求截止时间 */
    private LocalDateTime dueAt;

    /** CDP数仓隐私删除请求开始时间 */
    private LocalDateTime startedAt;

    /** CDP数仓隐私删除请求结束时间 */
    private LocalDateTime finishedAt;

    /** CDP数仓隐私删除请求目标资产明细 JSON */
    private String targetAssetsJson;

    /** CDP数仓隐私删除请求证据明细 JSON */
    private String evidenceJson;

    /** CDP数仓隐私删除请求创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓隐私删除请求最后更新时间 */
    private LocalDateTime updatedAt;
}
