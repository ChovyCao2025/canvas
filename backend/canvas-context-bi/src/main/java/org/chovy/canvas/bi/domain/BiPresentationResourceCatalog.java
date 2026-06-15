package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiBigScreenResourceCommand;
import org.chovy.canvas.bi.api.BiBigScreenResourceView;
import org.chovy.canvas.bi.api.BiPortalResourceCommand;
import org.chovy.canvas.bi.api.BiPortalResourceView;
import org.chovy.canvas.bi.api.BiResourceVersionView;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceCommand;
import org.chovy.canvas.bi.api.BiSpreadsheetResourceView;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BiPresentationResourceCatalog {

    private final Map<ResourceRef, PortalState> portals = new LinkedHashMap<>();
    private final Map<ResourceRef, BigScreenState> bigScreens = new LinkedHashMap<>();
    private final Map<ResourceRef, SpreadsheetState> spreadsheets = new LinkedHashMap<>();
    private final Map<ResourceRef, List<VersionState>> versions = new LinkedHashMap<>();

    public synchronized List<BiPortalResourceView> listPortals(Long tenantId) {
        Long scopedTenantId = tenant(tenantId);
        return portals.values().stream()
                .filter(state -> scopedTenantId.equals(state.tenantId))
                .filter(state -> !"ARCHIVED".equals(state.status))
                .sorted(Comparator.comparing((PortalState state) -> state.updatedAt).reversed()
                        .thenComparing(state -> state.portalKey))
                .map(this::toPortalView)
                .toList();
    }

    public synchronized BiPortalResourceView getPortal(Long tenantId, String portalKey) {
        PortalState state = portals.get(ref(tenantId, "PORTAL", portalKey));
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI portal resource not found");
        }
        return toPortalView(state);
    }

    public synchronized BiPortalResourceView savePortalDraft(
            Long tenantId,
            String portalKey,
            BiPortalResourceCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI portal resource command is required");
        }
        ResourceRef ref = ref(tenantId, "PORTAL", portalKey);
        PortalState existing = portals.get(ref);
        int version = existing == null ? 1 : existing.version + 1;
        PortalState state = new PortalState(
                ref.tenantId(),
                ref.resourceKey(),
                requiredText(command.title(), "title"),
                optionalText(command.description()),
                keys(command.dashboardKeys(), "dashboardKey"),
                copyMap(command.layout()),
                copyMap(command.settings()),
                "DRAFT",
                version,
                existing == null ? actor(actor) : existing.createdBy,
                actor(actor),
                existing == null ? now : existing.createdAt,
                now);
        portals.put(ref, state);
        appendVersion(ref, state.status, version, portalSnapshot(state), state.updatedBy, now);
        return toPortalView(state);
    }

    public synchronized BiPortalResourceView publishPortal(Long tenantId, String portalKey, String actor,
                                                           LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "PORTAL", portalKey);
        PortalState existing = requirePortal(ref);
        PortalState state = new PortalState(
                existing.tenantId,
                existing.portalKey,
                existing.title,
                existing.description,
                existing.dashboardKeys,
                existing.layout,
                existing.settings,
                "PUBLISHED",
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        portals.put(ref, state);
        appendVersion(ref, state.status, state.version, portalSnapshot(state), state.updatedBy, now);
        return toPortalView(state);
    }

    public synchronized void archivePortal(Long tenantId, String portalKey, String actor, LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "PORTAL", portalKey);
        PortalState existing = portals.get(ref);
        if (existing == null || "ARCHIVED".equals(existing.status)) {
            return;
        }
        portals.put(ref, new PortalState(
                existing.tenantId,
                existing.portalKey,
                existing.title,
                existing.description,
                existing.dashboardKeys,
                existing.layout,
                existing.settings,
                "ARCHIVED",
                existing.version,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now));
    }

    public synchronized List<BiResourceVersionView> listPortalVersions(Long tenantId, String portalKey) {
        return listVersions(ref(tenantId, "PORTAL", portalKey));
    }

    public synchronized BiPortalResourceView restorePortal(Long tenantId, String portalKey, Integer version,
                                                           String actor, LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "PORTAL", portalKey);
        PortalState existing = requirePortal(ref);
        VersionState target = version(ref, version);
        PortalState state = new PortalState(
                existing.tenantId,
                existing.portalKey,
                text(target.snapshot, "title"),
                text(target.snapshot, "description"),
                stringList(target.snapshot.get("dashboardKeys")),
                map(target.snapshot.get("layout")),
                map(target.snapshot.get("settings")),
                target.status,
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        portals.put(ref, state);
        appendVersion(ref, state.status, state.version, portalSnapshot(state), state.updatedBy, now);
        return toPortalView(state);
    }

    public synchronized List<BiBigScreenResourceView> listBigScreens(Long tenantId) {
        Long scopedTenantId = tenant(tenantId);
        return bigScreens.values().stream()
                .filter(state -> scopedTenantId.equals(state.tenantId))
                .filter(state -> !"ARCHIVED".equals(state.status))
                .sorted(Comparator.comparing((BigScreenState state) -> state.updatedAt).reversed()
                        .thenComparing(state -> state.screenKey))
                .map(this::toBigScreenView)
                .toList();
    }

    public synchronized BiBigScreenResourceView getBigScreen(Long tenantId, String screenKey) {
        BigScreenState state = bigScreens.get(ref(tenantId, "BIG_SCREEN", screenKey));
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI big-screen resource not found");
        }
        return toBigScreenView(state);
    }

    public synchronized BiBigScreenResourceView saveBigScreenDraft(
            Long tenantId,
            String screenKey,
            BiBigScreenResourceCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI big-screen resource command is required");
        }
        ResourceRef ref = ref(tenantId, "BIG_SCREEN", screenKey);
        BigScreenState existing = bigScreens.get(ref);
        int version = existing == null ? 1 : existing.version + 1;
        BigScreenState state = new BigScreenState(
                ref.tenantId(),
                ref.resourceKey(),
                requiredText(command.title(), "title"),
                optionalText(command.description()),
                keys(command.dashboardKeys(), "dashboardKey"),
                copyMap(command.layout()),
                copyMap(command.settings()),
                "DRAFT",
                version,
                existing == null ? actor(actor) : existing.createdBy,
                actor(actor),
                existing == null ? now : existing.createdAt,
                now);
        bigScreens.put(ref, state);
        appendVersion(ref, state.status, version, bigScreenSnapshot(state), state.updatedBy, now);
        return toBigScreenView(state);
    }

    public synchronized BiBigScreenResourceView publishBigScreen(Long tenantId, String screenKey, String actor,
                                                                 LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "BIG_SCREEN", screenKey);
        BigScreenState existing = requireBigScreen(ref);
        BigScreenState state = new BigScreenState(
                existing.tenantId,
                existing.screenKey,
                existing.title,
                existing.description,
                existing.dashboardKeys,
                existing.layout,
                existing.settings,
                "PUBLISHED",
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        bigScreens.put(ref, state);
        appendVersion(ref, state.status, state.version, bigScreenSnapshot(state), state.updatedBy, now);
        return toBigScreenView(state);
    }

    public synchronized void archiveBigScreen(Long tenantId, String screenKey, String actor, LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "BIG_SCREEN", screenKey);
        BigScreenState existing = bigScreens.get(ref);
        if (existing == null || "ARCHIVED".equals(existing.status)) {
            return;
        }
        bigScreens.put(ref, new BigScreenState(
                existing.tenantId,
                existing.screenKey,
                existing.title,
                existing.description,
                existing.dashboardKeys,
                existing.layout,
                existing.settings,
                "ARCHIVED",
                existing.version,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now));
    }

    public synchronized List<BiResourceVersionView> listBigScreenVersions(Long tenantId, String screenKey) {
        return listVersions(ref(tenantId, "BIG_SCREEN", screenKey));
    }

    public synchronized BiBigScreenResourceView restoreBigScreen(Long tenantId, String screenKey, Integer version,
                                                                 String actor, LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "BIG_SCREEN", screenKey);
        BigScreenState existing = requireBigScreen(ref);
        VersionState target = version(ref, version);
        BigScreenState state = new BigScreenState(
                existing.tenantId,
                existing.screenKey,
                text(target.snapshot, "title"),
                text(target.snapshot, "description"),
                stringList(target.snapshot.get("dashboardKeys")),
                map(target.snapshot.get("layout")),
                map(target.snapshot.get("settings")),
                target.status,
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        bigScreens.put(ref, state);
        appendVersion(ref, state.status, state.version, bigScreenSnapshot(state), state.updatedBy, now);
        return toBigScreenView(state);
    }

    public synchronized List<BiSpreadsheetResourceView> listSpreadsheets(Long tenantId) {
        Long scopedTenantId = tenant(tenantId);
        return spreadsheets.values().stream()
                .filter(state -> scopedTenantId.equals(state.tenantId))
                .filter(state -> !"ARCHIVED".equals(state.status))
                .sorted(Comparator.comparing((SpreadsheetState state) -> state.updatedAt).reversed()
                        .thenComparing(state -> state.spreadsheetKey))
                .map(this::toSpreadsheetView)
                .toList();
    }

    public synchronized BiSpreadsheetResourceView getSpreadsheet(Long tenantId, String spreadsheetKey) {
        SpreadsheetState state = spreadsheets.get(ref(tenantId, "SPREADSHEET", spreadsheetKey));
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI spreadsheet resource not found");
        }
        return toSpreadsheetView(state);
    }

    public synchronized BiSpreadsheetResourceView saveSpreadsheetDraft(
            Long tenantId,
            String spreadsheetKey,
            BiSpreadsheetResourceCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("BI spreadsheet resource command is required");
        }
        ResourceRef ref = ref(tenantId, "SPREADSHEET", spreadsheetKey);
        SpreadsheetState existing = spreadsheets.get(ref);
        int version = existing == null ? 1 : existing.version + 1;
        SpreadsheetState state = new SpreadsheetState(
                ref.tenantId(),
                ref.resourceKey(),
                requiredText(command.name(), "name"),
                optionalText(command.description()),
                copyMaps(command.sheets()),
                copyMap(command.dataBinding()),
                copyMap(command.style()),
                "DRAFT",
                version,
                existing == null ? actor(actor) : existing.createdBy,
                actor(actor),
                existing == null ? now : existing.createdAt,
                now);
        spreadsheets.put(ref, state);
        appendVersion(ref, state.status, version, spreadsheetSnapshot(state), state.updatedBy, now);
        return toSpreadsheetView(state);
    }

    public synchronized BiSpreadsheetResourceView publishSpreadsheet(Long tenantId, String spreadsheetKey, String actor,
                                                                     LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "SPREADSHEET", spreadsheetKey);
        SpreadsheetState existing = requireSpreadsheet(ref);
        SpreadsheetState state = new SpreadsheetState(
                existing.tenantId,
                existing.spreadsheetKey,
                existing.name,
                existing.description,
                existing.sheets,
                existing.dataBinding,
                existing.style,
                "PUBLISHED",
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        spreadsheets.put(ref, state);
        appendVersion(ref, state.status, state.version, spreadsheetSnapshot(state), state.updatedBy, now);
        return toSpreadsheetView(state);
    }

    public synchronized void archiveSpreadsheet(Long tenantId, String spreadsheetKey, String actor, LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "SPREADSHEET", spreadsheetKey);
        SpreadsheetState existing = spreadsheets.get(ref);
        if (existing == null || "ARCHIVED".equals(existing.status)) {
            return;
        }
        spreadsheets.put(ref, new SpreadsheetState(
                existing.tenantId,
                existing.spreadsheetKey,
                existing.name,
                existing.description,
                existing.sheets,
                existing.dataBinding,
                existing.style,
                "ARCHIVED",
                existing.version,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now));
    }

    public synchronized List<BiResourceVersionView> listSpreadsheetVersions(Long tenantId, String spreadsheetKey) {
        return listVersions(ref(tenantId, "SPREADSHEET", spreadsheetKey));
    }

    public synchronized BiSpreadsheetResourceView restoreSpreadsheet(Long tenantId,
                                                                     String spreadsheetKey,
                                                                     Integer version,
                                                                     String actor,
                                                                     LocalDateTime now) {
        ResourceRef ref = ref(tenantId, "SPREADSHEET", spreadsheetKey);
        SpreadsheetState existing = requireSpreadsheet(ref);
        VersionState target = version(ref, version);
        SpreadsheetState state = new SpreadsheetState(
                existing.tenantId,
                existing.spreadsheetKey,
                text(target.snapshot, "name"),
                text(target.snapshot, "description"),
                maps(target.snapshot.get("sheets")),
                map(target.snapshot.get("dataBinding")),
                map(target.snapshot.get("style")),
                "DRAFT",
                existing.version + 1,
                existing.createdBy,
                actor(actor),
                existing.createdAt,
                now);
        spreadsheets.put(ref, state);
        appendVersion(ref, state.status, state.version, spreadsheetSnapshot(state), state.updatedBy, now);
        return toSpreadsheetView(state);
    }

    private PortalState requirePortal(ResourceRef ref) {
        PortalState state = portals.get(ref);
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI portal resource not found");
        }
        return state;
    }

    private BigScreenState requireBigScreen(ResourceRef ref) {
        BigScreenState state = bigScreens.get(ref);
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI big-screen resource not found");
        }
        return state;
    }

    private SpreadsheetState requireSpreadsheet(ResourceRef ref) {
        SpreadsheetState state = spreadsheets.get(ref);
        if (state == null || "ARCHIVED".equals(state.status)) {
            throw new IllegalArgumentException("BI spreadsheet resource not found");
        }
        return state;
    }

    private List<BiResourceVersionView> listVersions(ResourceRef ref) {
        return versions.getOrDefault(ref, List.of()).stream()
                .sorted(Comparator.comparing(VersionState::version).reversed())
                .map(state -> new BiResourceVersionView(
                        ref.resourceType(),
                        ref.resourceKey(),
                        state.version(),
                        state.status(),
                        state.snapshot(),
                        state.createdBy(),
                        state.createdAt()))
                .toList();
    }

    private VersionState version(ResourceRef ref, Integer version) {
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version is required");
        }
        return versions.getOrDefault(ref, List.of()).stream()
                .filter(state -> version.equals(state.version()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("BI resource version not found"));
    }

    private void appendVersion(ResourceRef ref,
                               String status,
                               Integer version,
                               Map<String, Object> snapshot,
                               String actor,
                               LocalDateTime now) {
        List<VersionState> existing = versions.getOrDefault(ref, List.of());
        List<VersionState> updated = new java.util.ArrayList<>(existing);
        updated.add(new VersionState(version, status, snapshot, actor, now));
        versions.put(ref, List.copyOf(updated));
    }

    private BiPortalResourceView toPortalView(PortalState state) {
        return new BiPortalResourceView(
                state.tenantId,
                state.portalKey,
                state.title,
                state.description,
                state.dashboardKeys,
                state.layout,
                state.settings,
                state.status,
                state.version,
                state.createdBy,
                state.updatedBy,
                state.createdAt,
                state.updatedAt);
    }

    private BiBigScreenResourceView toBigScreenView(BigScreenState state) {
        return new BiBigScreenResourceView(
                state.tenantId,
                state.screenKey,
                state.title,
                state.description,
                state.dashboardKeys,
                state.layout,
                state.settings,
                state.status,
                state.version,
                state.createdBy,
                state.updatedBy,
                state.createdAt,
                state.updatedAt);
    }

    private BiSpreadsheetResourceView toSpreadsheetView(SpreadsheetState state) {
        return new BiSpreadsheetResourceView(
                state.tenantId,
                state.spreadsheetKey,
                state.name,
                state.description,
                state.sheets,
                state.dataBinding,
                state.style,
                state.status,
                state.version,
                state.createdBy,
                state.updatedBy,
                state.createdAt,
                state.updatedAt);
    }

    private static Map<String, Object> portalSnapshot(PortalState state) {
        return Map.of(
                "title", state.title,
                "description", state.description == null ? "" : state.description,
                "dashboardKeys", state.dashboardKeys,
                "layout", state.layout,
                "settings", state.settings);
    }

    private static Map<String, Object> bigScreenSnapshot(BigScreenState state) {
        return Map.of(
                "title", state.title,
                "description", state.description == null ? "" : state.description,
                "dashboardKeys", state.dashboardKeys,
                "layout", state.layout,
                "settings", state.settings);
    }

    private static Map<String, Object> spreadsheetSnapshot(SpreadsheetState state) {
        return Map.of(
                "name", state.name,
                "description", state.description == null ? "" : state.description,
                "sheets", state.sheets,
                "dataBinding", state.dataBinding,
                "style", state.style);
    }

    private static ResourceRef ref(Long tenantId, String resourceType, String resourceKey) {
        return new ResourceRef(tenant(tenantId), resourceType, BiResourceKey.of(resourceKey, "resourceKey").value());
    }

    private static Long tenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static List<String> keys(List<String> values, String field) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> BiResourceKey.of(value, field).value())
                .distinct()
                .toList();
    }

    private static Map<String, Object> copyMap(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    private static List<Map<String, Object>> copyMaps(List<Map<String, Object>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(BiPresentationResourceCatalog::copySpreadsheetSheet)
                .toList();
    }

    private static Map<String, Object> copySpreadsheetSheet(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>(value);
        Object sheetKey = copy.get("sheetKey");
        if (sheetKey instanceof String key && !key.isBlank()) {
            copy.put("sheetKey", key.trim());
        }
        return Map.copyOf(copy);
    }

    private static String text(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private record ResourceRef(Long tenantId, String resourceType, String resourceKey) {
    }

    private record VersionState(
            Integer version,
            String status,
            Map<String, Object> snapshot,
            String createdBy,
            LocalDateTime createdAt) {
    }

    private record PortalState(
            Long tenantId,
            String portalKey,
            String title,
            String description,
            List<String> dashboardKeys,
            Map<String, Object> layout,
            Map<String, Object> settings,
            String status,
            Integer version,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    private record BigScreenState(
            Long tenantId,
            String screenKey,
            String title,
            String description,
            List<String> dashboardKeys,
            Map<String, Object> layout,
            Map<String, Object> settings,
            String status,
            Integer version,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    private record SpreadsheetState(
            Long tenantId,
            String spreadsheetKey,
            String name,
            String description,
            List<Map<String, Object>> sheets,
            Map<String, Object> dataBinding,
            Map<String, Object> style,
            String status,
            Integer version,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
