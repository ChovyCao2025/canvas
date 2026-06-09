package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehousePrivacySubjectTombstoneDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_privacy_subject_tombstone")
public class CdpWarehousePrivacySubjectTombstoneDO {

    /** CDP数仓隐私主体墓碑主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓隐私主体墓碑主体类型 */
    private String subjectType;

    /** CDP数仓隐私主体墓碑主体哈希 */
    private String subjectHash;

    /** CDP数仓隐私主体墓碑主体REFMASKED */
    private String subjectRefMasked;

    /** CDP数仓隐私主体墓碑当前状态 */
    private String status;

    /** 关联的来源请求 ID */
    private Long sourceRequestId;

    /** CDP数仓隐私主体墓碑来源请求业务键 */
    private String sourceRequestKey;

    /** CDP数仓隐私主体墓碑原因说明 */
    private String reason;

    /** CDP数仓隐私主体墓碑BLOCKEDEVENTCOUNT数量 */
    private Long blockedEventCount;

    /** CDP数仓隐私主体墓碑最近阻塞时间 */
    private LocalDateTime lastBlockedAt;

    /** CDP数仓隐私主体墓碑创建人 */
    private String createdBy;

    /** CDP数仓隐私主体墓碑吊销人 */
    private String revokedBy;

    /** CDP数仓隐私主体墓碑吊销时间 */
    private LocalDateTime revokedAt;

    /** CDP数仓隐私主体墓碑创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓隐私主体墓碑最后更新时间 */
    private LocalDateTime updatedAt;
}
