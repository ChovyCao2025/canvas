package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销授权 数据对象，对应数据库表 {@code marketing_consent}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("marketing_consent")
public class MarketingConsentDO {
    /** 用户已授权接收营销触达 */
    public static final String OPT_IN = "OPT_IN";

    /** 用户已拒绝接收营销触达 */
    public static final String OPT_OUT = "OPT_OUT";

    @TableId(type = IdType.AUTO)
    /** 营销授权记录主键 ID */
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 业务用户 ID */
    private String userId;

    /** 授权渠道，如 SMS、EMAIL、PUSH、WECHAT */
    private String channel;

    /** 授权状态，取值见 OPT_IN/OPT_OUT 常量 */
    private String consentStatus;

    /** 授权来源，如用户设置、导入、活动表单 */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
