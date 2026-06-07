package org.chovy.canvas.domain.bi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Service
public class BiDatasourceFileUploadService {

    private static final long DEFAULT_MAX_BYTES = 20L * 1024L * 1024L;

    private final BiDatasourceOnboardingService onboardingService;
    private final Path uploadRoot;
    private final long maxBytes;

    @Autowired
    public BiDatasourceFileUploadService(BiDatasourceOnboardingService onboardingService) {
        this(onboardingService, "tmp/bi-datasource-uploads", DEFAULT_MAX_BYTES);
    }

    public BiDatasourceFileUploadService(BiDatasourceOnboardingService onboardingService,
                                         String uploadRoot,
                                         long maxBytes) {
        this.onboardingService = onboardingService;
        this.uploadRoot = Path.of(uploadRoot == null || uploadRoot.isBlank()
                ? "tmp/bi-datasource-uploads"
                : uploadRoot);
        this.maxBytes = maxBytes <= 0 ? DEFAULT_MAX_BYTES : maxBytes;
    }

    public BiDatasourceOnboardingView upload(Long tenantId,
                                             String operator,
                                             String originalFileName,
                                             byte[] content,
                                             BiDatasourceFileUploadCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI datasource file upload command is required");
        }
        byte[] bytes = content == null ? new byte[0] : content;
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("uploaded file exceeds BI datasource upload limit");
        }
        String fileName = safeFileName(originalFileName);
        String fileType = fileType(fileName);
        Path tenantRoot = uploadRoot.resolve("tenant-" + normalizeTenant(tenantId)).normalize();
        Path stored = tenantRoot.resolve(storedFileName(fileName)).normalize();
        if (!stored.startsWith(tenantRoot)) {
            throw new IllegalArgumentException("invalid BI datasource upload file name");
        }
        try {
            Files.createDirectories(tenantRoot);
            Files.write(stored, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to store BI datasource upload", ex);
        }
        return onboardingService.createOnboardingSource(
                tenantId,
                operator,
                new BiDatasourceOnboardingCommand(
                        "CSV_EXCEL",
                        required(command.name(), "name"),
                        stored.toUri().toString(),
                        "file_upload",
                        "",
                        "FILE_UPLOAD",
                        command.description(),
                        true,
                        "EXTRACT",
                        connectorConfig(command, fileName, fileType)));
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String safeFileName(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String baseName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        baseName = baseName.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
        if (baseName.isBlank() || ".".equals(baseName) || "..".equals(baseName)) {
            throw new IllegalArgumentException("BI datasource upload file name is required");
        }
        fileType(baseName);
        return baseName;
    }

    private static String storedFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        return stem + "-" + System.nanoTime() + extension;
    }

    private static String fileType(String fileName) {
        String value = extension(fileName);
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("XLSX".equals(type) || "XLS".equals(type) || "CSV".equals(type)) {
            return type;
        }
        throw new IllegalArgumentException("unsupported BI datasource upload file type");
    }

    private static String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : "";
    }

    private static String required(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static Map<String, Object> connectorConfig(BiDatasourceFileUploadCommand command,
                                                       String fileName,
                                                       String fileType) {
        java.util.LinkedHashMap<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("fileName", fileName);
        config.put("fileType", fileType);
        String sheetName = defaultString(command.sheetName(), "");
        if (!sheetName.isBlank()) {
            config.put("sheetName", sheetName);
        }
        config.put("delimiter", defaultString(command.delimiter(), ","));
        config.put("headerRow", command.headerRow() == null || command.headerRow());
        config.put("encoding", defaultString(command.encoding(), "UTF-8"));
        return config;
    }
}
