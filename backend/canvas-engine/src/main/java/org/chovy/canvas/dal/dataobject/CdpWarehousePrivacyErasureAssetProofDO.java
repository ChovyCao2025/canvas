package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehousePrivacyErasureAssetProofDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_privacy_erasure_asset_proof")
public class CdpWarehousePrivacyErasureAssetProofDO {

    /** CDP数仓隐私删除资产证明主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的请求 ID */
    private Long requestId;

    /** CDP数仓隐私删除资产证明请求业务键 */
    private String requestKey;

    /** CDP数仓隐私删除资产证明资产业务键 */
    private String assetKey;

    /** CDP数仓隐私删除资产证明资产流量层 */
    private String assetLayer;

    /** CDP数仓隐私删除资产证明动作类型 */
    private String actionType;

    /** CDP数仓隐私删除资产证明当前状态 */
    private String status;

    /** CDP数仓隐私删除资产证明计划动作 */
    private String plannedAction;

    /** CDP数仓隐私删除资产证明匹配数量 */
    private Long matchedCount;

    /** CDP数仓隐私删除资产证明受影响数量 */
    private Long affectedCount;

    /** CDP数仓隐私删除资产证明证明消息 */
    private String proofMessage;

    /** CDP数仓隐私删除资产证明错误信息 */
    private String errorMessage;

    /** CDP数仓隐私删除资产证明执行人 */
    private String executedBy;

    /** CDP数仓隐私删除资产证明执行时间 */
    private LocalDateTime executedAt;

    /** CDP数仓隐私删除资产证明创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓隐私删除资产证明最后更新时间 */
    private LocalDateTime updatedAt;
}
