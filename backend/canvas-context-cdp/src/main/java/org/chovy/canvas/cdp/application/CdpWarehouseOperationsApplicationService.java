package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseOperationsFacade;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseOperationsApplicationService implements CdpWarehouseOperationsFacade {

    private static final int MAX_BACKFILL_LIMIT = 5000;
    private static final int MAX_STATUS_LIMIT = 100;

    @Override
    public Map<String, Object> status(Long tenantId, int limit) {
        Long scopedTenantId = tenantIdOrDefault(tenantId);
        Map<String, Object> result = ordered();
        result.put("tenantId", scopedTenantId);
        result.put("runs", List.of());
        result.put("watermarks", List.of());
        result.put("limit", boundStatusLimit(limit));
        return result;
    }

    @Override
    public Map<String, Object> triggerBackfill(Long tenantId, Long lastId, int limit, String operator) {
        requireBackfillLimit(limit);
        long startId = lastId == null ? 0L : lastId;
        Map<String, Object> result = ordered();
        result.put("status", "SUCCESS");
        result.put("loaded", 0L);
        result.put("failed", 0L);
        result.put("lastEventId", startId);
        result.put("tenantId", tenantIdOrDefault(tenantId));
        result.put("lastId", startId);
        result.put("limit", limit);
        result.put("operator", operatorOrDefault(operator));
        return result;
    }

    @Override
    public Map<String, Object> triggerAggregation(Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        validateWindow(from, to);
        Map<String, Object> result = ordered();
        result.put("status", "SKIPPED");
        result.put("dwdRows", 0);
        result.put("dwsRows", 0);
        result.put("tenantId", tenantIdOrDefault(tenantId));
        result.put("from", from);
        result.put("to", to);
        result.put("operator", operatorOrDefault(operator));
        return result;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static int boundStatusLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_STATUS_LIMIT);
    }

    private static void requireBackfillLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (limit > MAX_BACKFILL_LIMIT) {
            throw new IllegalArgumentException("limit must be <= " + MAX_BACKFILL_LIMIT);
        }
    }

    private static void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private static String operatorOrDefault(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
