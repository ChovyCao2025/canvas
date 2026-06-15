package org.chovy.canvas.canvas.domain;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionCommand;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionListQuery;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionView;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.PageView;

public class ApiDefinitionCatalog {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, ApiRow> rows = new LinkedHashMap<>();

    public synchronized PageView<ApiDefinitionView> list(ApiDefinitionListQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        List<ApiDefinitionView> filtered = rows.values().stream()
                .filter(row -> query.enabled() == null || query.enabled().equals(row.enabled))
                .sorted(Comparator.comparing((ApiRow row) -> row.id).reversed())
                .map(ApiDefinitionCatalog::view)
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageView<>(filtered.size(), filtered.subList(from, to));
    }

    public synchronized ApiDefinitionView create(ApiDefinitionCommand command) {
        ApiDefinitionCommand safe = requireCommand(command);
        validateRateLimit(safe.rateLimitPerSec());
        validateOutboundUrl(safe.url(), true);
        ApiRow row = new ApiRow();
        row.id = ids.getAndIncrement();
        row.apiKey = safe.apiKey();
        row.url = safe.url();
        row.enabled = safe.enabled() == null ? 1 : safe.enabled();
        row.includeContextPayload = safe.includeContextPayload() == null ? 0 : safe.includeContextPayload();
        row.receiptEnabled = safe.receiptEnabled() == null ? 0 : safe.receiptEnabled();
        row.receiptExpireMinutes = safe.receiptExpireMinutes() == null ? 1440 : safe.receiptExpireMinutes();
        row.receiptStatuses = safe.receiptStatuses() == null ? "[]" : safe.receiptStatuses();
        row.rateLimitPerSec = safe.rateLimitPerSec();
        rows.put(row.id, row);
        return view(row);
    }

    public synchronized ApiDefinitionView update(Long id, ApiDefinitionCommand command) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        ApiRow row = rows.get(id);
        if (row == null) {
            throw new IllegalArgumentException("api definition not found: " + id);
        }
        ApiDefinitionCommand safe = requireCommand(command);
        validateRateLimit(safe.rateLimitPerSec());
        validateOutboundUrl(safe.url(), false);
        if (safe.apiKey() != null) {
            row.apiKey = safe.apiKey();
        }
        if (safe.url() != null) {
            row.url = safe.url();
        }
        if (safe.enabled() != null) {
            row.enabled = safe.enabled();
        }
        if (safe.includeContextPayload() != null) {
            row.includeContextPayload = safe.includeContextPayload();
        }
        if (safe.receiptEnabled() != null) {
            row.receiptEnabled = safe.receiptEnabled();
        }
        if (safe.receiptExpireMinutes() != null) {
            row.receiptExpireMinutes = safe.receiptExpireMinutes();
        }
        if (safe.receiptStatuses() != null) {
            row.receiptStatuses = safe.receiptStatuses();
        }
        if (safe.rateLimitPerSecPresent()) {
            row.rateLimitPerSec = safe.rateLimitPerSec();
        }
        return view(row);
    }

    public synchronized void delete(Long id) {
        rows.remove(id);
    }

    private static ApiDefinitionCommand requireCommand(ApiDefinitionCommand command) {
        if (command == null) {
            return new ApiDefinitionCommand(null, null, null, null, null, null, null, null, false);
        }
        return command;
    }

    private static void validateRateLimit(Integer rateLimitPerSec) {
        if (rateLimitPerSec != null && rateLimitPerSec <= 0) {
            throw new IllegalArgumentException("rateLimitPerSec 必须大于 0");
        }
    }

    private static void validateOutboundUrl(String url, boolean required) {
        if (url == null) {
            if (required) {
                throw new IllegalArgumentException("url is required");
            }
            return;
        }
        if (url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("url 格式不合法", e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("url 仅支持 http/https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("url host 不能为空");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("url 不允许包含用户信息");
        }
        if (isBlockedHost(IDN.toASCII(host))) {
            throw new IllegalArgumentException("url host 不允许访问内网或本机地址");
        }
    }

    private static boolean isBlockedHost(String host) {
        String normalized = host.toLowerCase();
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost")
                || "0.0.0.0".equals(normalized) || "::1".equals(normalized)
                || normalized.startsWith("127.") || normalized.startsWith("169.254.")
                || normalized.startsWith("10.") || normalized.startsWith("192.168.")) {
            return true;
        }
        if (normalized.startsWith("172.")) {
            String[] parts = normalized.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return normalized.startsWith("fc") || normalized.startsWith("fd") || normalized.startsWith("fe80:");
    }

    private static ApiDefinitionView view(ApiRow row) {
        return new ApiDefinitionView(
                row.id,
                row.apiKey,
                row.url,
                row.enabled,
                row.includeContextPayload,
                row.receiptEnabled,
                row.receiptExpireMinutes,
                row.receiptStatuses,
                row.rateLimitPerSec);
    }

    private static final class ApiRow {
        private Long id;
        private String apiKey;
        private String url;
        private Integer enabled;
        private Integer includeContextPayload;
        private Integer receiptEnabled;
        private Integer receiptExpireMinutes;
        private String receiptStatuses;
        private Integer rateLimitPerSec;
    }
}
