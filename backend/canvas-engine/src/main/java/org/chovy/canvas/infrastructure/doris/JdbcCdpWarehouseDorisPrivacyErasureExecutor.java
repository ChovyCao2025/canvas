package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.domain.warehouse.CdpWarehouseDorisPrivacyErasureExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class JdbcCdpWarehouseDorisPrivacyErasureExecutor implements CdpWarehouseDorisPrivacyErasureExecutor {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";

    private final JdbcTemplate dorisJdbcTemplate;

    public JdbcCdpWarehouseDorisPrivacyErasureExecutor(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate) {
        this.dorisJdbcTemplate = dorisJdbcTemplate.getIfAvailable();
    }

    @Override
    public Result execute(Command command) {
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
        return new Result(STATUS_PASS, matched, affected,
                "deleted Doris rows for " + command.assetKey(), null);
    }

    private String table(String assetKey) {
        return switch (normalize(assetKey)) {
            case "DORIS_ODS_CDP_EVENT_LOG" -> "canvas_ods.cdp_event_log";
            case "DORIS_DWD_CDP_USER_EVENT_FACT" -> "canvas_dwd.cdp_user_event_fact";
            default -> null;
        };
    }

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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
