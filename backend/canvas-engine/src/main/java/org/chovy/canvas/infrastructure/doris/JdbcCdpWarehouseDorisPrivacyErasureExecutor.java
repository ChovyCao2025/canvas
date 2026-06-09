package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.domain.warehouse.CdpWarehouseDorisPrivacyErasureExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * JdbcCdpWarehouseDorisPrivacyErasureExecutor 封装 infrastructure.doris 场景的基础设施集成。
 */
@Service
public class JdbcCdpWarehouseDorisPrivacyErasureExecutor implements CdpWarehouseDorisPrivacyErasureExecutor {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";

    private final JdbcTemplate dorisJdbcTemplate;

    /**
     * 创建 JdbcCdpWarehouseDorisPrivacyErasureExecutor 实例并注入 infrastructure.doris 场景依赖。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcCdpWarehouseDorisPrivacyErasureExecutor 流程中的校验、计算或对象转换。
     */
    public JdbcCdpWarehouseDorisPrivacyErasureExecutor(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate.getIfAvailable();
    }

    /**
     * 在 Doris 中执行隐私删除或删除预检。
     *
     * <p>方法根据资产 key 和主体类型映射允许删除的 Doris 表与列，先统计匹配行数；dry-run 只返回匹配数量，
     * 非 dry-run 会执行 DELETE 并返回影响行数。未配置 Doris JDBC 或资产不支持时返回 WARN/FAIL 结果，不直接抛出。</p>
     *
     * @param command 删除命令，包含租户、资产、主体类型、主体值和 dry-run 标记
     * @return 删除执行状态、匹配行数、实际删除行数和诊断信息
     */
    @Override
    public Result execute(Command command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("Doris erasure command is required");
        }
        if (dorisJdbcTemplate == null) {
            return new Result(command.dryRun() ? STATUS_WARN : STATUS_FAIL, 0, 0,
                    "Doris JDBC template is not configured",
                    "Doris JDBC template is not configured");
        }
        String table = table(command.assetKey());
        String column = column(command.assetKey(), command.subjectType());
        if (table == null || column == null) {
            return new Result(command.dryRun() ? STATUS_WARN : STATUS_FAIL, 0, 0,
                    "Doris asset or subject type is not executable",
                    "unsupported Doris erasure asset or subject type");
        }

        String whereSql = " WHERE tenant_id = ? AND " + column + " = ?";
        String countSql = "SELECT COUNT(*) FROM " + table + whereSql;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        long matched = nullToZero(dorisJdbcTemplate.queryForObject(
                countSql, Long.class, command.tenantId(), command.subjectValue()));
        if (command.dryRun()) {
            return new Result(STATUS_WARN, matched, 0,
                    "dry-run matched Doris rows for " + command.assetKey(), null);
        }
        int affected = 0;
        if (matched > 0) {
            affected = dorisJdbcTemplate.update("DELETE FROM " + table + whereSql,
                    command.tenantId(), command.subjectValue());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new Result(STATUS_PASS, matched, affected,
                "deleted Doris rows for " + command.assetKey(), null);
    }

    /**
     * 执行 table 流程，围绕 table 完成校验、计算或结果组装。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 table 生成的文本或业务键。
     */
    private String table(String assetKey) {
        return switch (normalize(assetKey)) {
            case "DORIS_ODS_CDP_EVENT_LOG" -> "canvas_ods.cdp_event_log";
            case "DORIS_DWD_CDP_USER_EVENT_FACT" -> "canvas_dwd.cdp_user_event_fact";
            default -> null;
        };
    }

    /**
     * 执行 column 流程，围绕 column 完成校验、计算或结果组装。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param subjectType 类型标识，用于选择对应处理分支。
     * @return 返回 column 生成的文本或业务键。
     */
    private String column(String assetKey, String subjectType) {
        String asset = normalize(assetKey);
        String subject = normalize(subjectType);
        if ("DORIS_DWD_CDP_USER_EVENT_FACT".equals(asset)) {
            return "USER_ID".equals(subject) ? "user_id" : null;
        }
        return switch (subject) {
            case "USER_ID" -> "user_id";
            case "ANONYMOUS_ID" -> "anonymous_id";
            case "DEVICE_ID" -> "device_id";
            default -> null;
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to zero 计算得到的数量、金额或指标值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
