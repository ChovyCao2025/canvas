package org.chovy.canvas.domain.bi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * BiDatasourceFileUploadService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiDatasourceFileUploadService {

    private static final long DEFAULT_MAX_BYTES = 20L * 1024L * 1024L;

    private final BiDatasourceOnboardingService onboardingService;
    private final Path uploadRoot;
    private final long maxBytes;

    @Autowired
    /**
     * 初始化 BiDatasourceFileUploadService 实例。
     *
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceFileUploadService(BiDatasourceOnboardingService onboardingService) {
        this(onboardingService, "tmp/bi-datasource-uploads", DEFAULT_MAX_BYTES);
    }

    /**
     * 初始化 BiDatasourceFileUploadService 实例。
     *
     * @param onboardingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param uploadRoot upload root 参数，用于 BiDatasourceFileUploadService 流程中的校验、计算或对象转换。
     * @param maxBytes max bytes 参数，用于 BiDatasourceFileUploadService 流程中的校验、计算或对象转换。
     */
    public BiDatasourceFileUploadService(BiDatasourceOnboardingService onboardingService,
                                         String uploadRoot,
                                         long maxBytes) {
        this.onboardingService = onboardingService;
        this.uploadRoot = Path.of(uploadRoot == null || uploadRoot.isBlank()
                ? "tmp/bi-datasource-uploads"
                : uploadRoot);
        this.maxBytes = maxBytes <= 0 ? DEFAULT_MAX_BYTES : maxBytes;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param originalFileName 名称文本，用于展示或唯一性校验。
     * @param content content 参数，用于 upload 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 upload 流程生成的业务结果。
     */
    public BiDatasourceOnboardingView upload(Long tenantId,
                                             String operator,
                                             String originalFileName,
                                             byte[] content,
                                             BiDatasourceFileUploadCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe file name 生成的文本或业务键。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 stored file name 生成的文本或业务键。
     */
    private static String storedFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        return stem + "-" + System.nanoTime() + extension;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 file type 生成的文本或业务键。
     */
    private static String fileType(String fileName) {
        String value = extension(fileName);
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("XLSX".equals(type) || "XLS".equals(type) || "CSV".equals(type)) {
            return type;
        }
        throw new IllegalArgumentException("unsupported BI datasource upload file type");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回 extension 生成的文本或业务键。
     */
    private static String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : "";
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @param fileType 类型标识，用于选择对应处理分支。
     * @return 返回 connectorConfig 流程生成的业务结果。
     */
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
