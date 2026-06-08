package org.chovy.canvas.domain.bi.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HttpS3ObjectClient 编排 domain.bi.storage 场景的领域业务规则。
 */
public class HttpS3ObjectClient implements S3ObjectClient {

    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final HexFormat HEX = HexFormat.of();

    private final S3CompatibleBiStorageProperties properties;
    private final HttpClient httpClient;
    private final Clock clock;

    /**
     * 创建 HttpS3ObjectClient 实例并注入 domain.bi.storage 场景依赖。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     */
    public HttpS3ObjectClient(S3CompatibleBiStorageProperties properties, HttpClient httpClient) {
        this(properties, httpClient, Clock.systemUTC());
    }

    /**
     * 执行 HttpS3ObjectClient 流程，围绕 http s3 object client 完成校验、计算或结果组装。
     *
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param httpClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    HttpS3ObjectClient(S3CompatibleBiStorageProperties properties, HttpClient httpClient, Clock clock) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    /**
     * 使用 S3 Signature V4 PUT 上传对象。
     *
     * @param request bucket 和对象 key
     * @param bytes 对象内容；为空时上传空对象
     */
    @Override
    public void putObject(S3ObjectRequest request, byte[] bytes) {
        byte[] payload = bytes == null ? new byte[0] : bytes;
        HttpResponse<byte[]> response = exchange("PUT", request, payload);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 putObject failed with status " + status + " for " + request.objectKey());
        }
    }

    /**
     * 使用 S3 Signature V4 GET 读取对象。
     *
     * @param request bucket 和对象 key
     * @return 对象内容；当对象不存在时返回 {@code null}
     */
    @Override
    public byte[] getObject(S3ObjectRequest request) {
        HttpResponse<byte[]> response = exchange("GET", request, new byte[0]);
        int status = response.statusCode();
        if (status == 404) {
            return null;
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 getObject failed with status " + status + " for " + request.objectKey());
        }
        return response.body();
    }

    /**
     * 使用 S3 Signature V4 DELETE 删除对象。
     *
     * @param request bucket 和对象 key
     * @return {@code true} 表示对象存在并被删除，{@code false} 表示对象不存在
     */
    @Override
    public boolean deleteObject(S3ObjectRequest request) {
        HttpResponse<byte[]> response = exchange("DELETE", request, new byte[0]);
        int status = response.statusCode();
        if (status == 404) {
            return false;
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 deleteObject failed with status " + status + " for " + request.objectKey());
        }
        return true;
    }

    /**
     * 写入 bucket 生命周期规则，用于自动清理 BI 导出对象和订阅附件。
     *
     * @param request bucket 请求
     * @param lifecycleXml S3 Lifecycle XML；空值按空 XML 内容发送
     */
    @Override
    public void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
        byte[] payload = (lifecycleXml == null ? "" : lifecycleXml).getBytes(StandardCharsets.UTF_8);
        HttpResponse<byte[]> response = exchange("PUT", bucketLifecycleUri(request), request.bucket(), payload);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("S3 putBucketLifecycle failed with status " + status
                    + " for bucket " + request.bucket());
        }
    }

    /**
     * 构造、签名并发送对象级 HTTP 请求。
     */
    private HttpResponse<byte[]> exchange(String method, S3ObjectRequest request, byte[] payload) {
        return exchange(method, objectUri(request), request.objectKey(), payload);
    }

    /**
     * 统一执行 S3 请求：计算 payload hash、生成日期戳、签名 Authorization 并处理中断语义。
     */
    private HttpResponse<byte[]> exchange(String method, URI uri, String target, byte[] payload) {
        try {
            String payloadHash = sha256Hex(payload);
            Instant now = clock.instant();
            String amzDate = AMZ_DATE.format(now);
            String dateStamp = DATE_STAMP.format(now);
            String host = hostHeader(uri);
            String authorization = authorization(method, uri, host, payloadHash, amzDate, dateStamp);

            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(properties.requestTimeoutMs()))
                    .header("Authorization", authorization)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("x-amz-date", amzDate);
            HttpRequest httpRequest = switch (method) {
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
                case "GET" -> builder.GET().build();
                case "DELETE" -> builder.DELETE().build();
                default -> throw new IllegalArgumentException("unsupported S3 method: " + method);
            };
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("S3 request failed for " + target, e);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("S3 request interrupted for " + target, e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 objectUri 流程生成的业务结果。
     */
    private URI objectUri(S3ObjectRequest request) {
        URI endpoint = URI.create(properties.endpoint());
        String objectPath = encodeObjectKey(request.objectKey());
        String basePath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        try {
            if (properties.pathStyle()) {
                String path = basePath + "/" + encodeSegment(request.bucket()) + "/" + objectPath;
                return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), path, null, null);
            }
            String host = request.bucket() + "." + endpoint.getHost();
            String path = basePath + "/" + objectPath;
            return new URI(endpoint.getScheme(), null, host, endpoint.getPort(), path, null, null);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid S3 endpoint or object key", e);
        }
    }

    /**
     * 构造 bucket 生命周期接口 URI。
     *
     * <p>方法同时支持 path-style 和 virtual-hosted-style endpoint，并把生命周期操作固定为
     * {@code ?lifecycle} 查询参数，后续签名逻辑会把它规范化为 {@code lifecycle=}。</p>
     */
    private URI bucketLifecycleUri(S3BucketLifecycleRequest request) {
        URI endpoint = URI.create(properties.endpoint());
        String basePath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        while (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        try {
            if (properties.pathStyle()) {
                String path = basePath + "/" + encodeSegment(request.bucket());
                return new URI(endpoint.getScheme(), null, endpoint.getHost(), endpoint.getPort(), path, "lifecycle", null);
            }
            String host = request.bucket() + "." + endpoint.getHost();
            String path = basePath.isBlank() ? "/" : basePath + "/";
            return new URI(endpoint.getScheme(), null, host, endpoint.getPort(), path, "lifecycle", null);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid S3 endpoint or bucket lifecycle request", e);
        }
    }

    /**
     * 生成 S3 Signature V4 Authorization 头。
     *
     * <p>签名只覆盖 host、payload hash 和 x-amz-date 三个头，canonical path/query 必须与实际请求 URI 一致；
     * 该方法是对象读写和生命周期配置共享的安全边界。</p>
     */
    private String authorization(String method,
                                 URI uri,
                                 String host,
                                 String payloadHash,
                                 String amzDate,
                                 String dateStamp) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("x-amz-date", amzDate);
        StringBuilder canonicalHeaders = new StringBuilder();
        headers.forEach((key, value) -> canonicalHeaders.append(key).append(':').append(value).append('\n'));
        String signedHeaders = String.join(";", headers.keySet());
        String canonicalRequest = method + "\n"
                /**
                 * 判断业务条件是否成立。
                 *
                 * @param uri uri 参数，用于 canonicalPath 流程中的校验、计算或对象转换。
                 * @return 返回布尔判断结果。
                 */
                + canonicalPath(uri) + "\n"
                /**
                 * 判断业务条件是否成立。
                 *
                 * @param uri uri 参数，用于 canonicalQuery 流程中的校验、计算或对象转换。
                 * @return 返回布尔判断结果。
                 */
                + canonicalQuery(uri) + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;
        String scope = dateStamp + "/" + properties.region() + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + scope + "\n"
                /**
                 * 执行 sha256Hex 流程，围绕 sha256 hex 完成校验、计算或结果组装。
                 *
                 * @return 返回 sha256Hex 流程生成的业务结果。
                 */
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String signature = HEX.formatHex(hmac(signingKey(dateStamp), stringToSign));
        return "AWS4-HMAC-SHA256 Credential=" + properties.accessKey() + "/" + scope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    /**
     * 按 AWS4-HMAC-SHA256 派生日期、区域、服务和请求级签名密钥。
     */
    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmac(("AWS4" + properties.secretKey()).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] regionKey = hmac(dateKey, properties.region());
        byte[] serviceKey = hmac(regionKey, "s3");
        return hmac(serviceKey, "aws4_request");
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param uri uri 参数，用于 canonicalPath 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static String canonicalPath(URI uri) {
        String rawPath = uri.getRawPath();
        return rawPath == null || rawPath.isBlank() ? "/" : rawPath;
    }

    /**
     * 返回签名使用的规范化查询串。
     *
     * <p>S3 lifecycle API 的 URI 查询为无值参数，签名时必须写成 {@code lifecycle=}，
     * 否则部分兼容对象存储会拒绝请求。</p>
     */
    private static String canonicalQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        if ("lifecycle".equals(rawQuery)) {
            return "lifecycle=";
        }
        return rawQuery;
    }

    /**
     * 执行 hostHeader 流程，围绕 host header 完成校验、计算或结果组装。
     *
     * @param uri uri 参数，用于 hostHeader 流程中的校验、计算或对象转换。
     * @return 返回 host header 生成的文本或业务键。
     */
    private static String hostHeader(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        if (port < 0 || (port == 443 && "https".equalsIgnoreCase(uri.getScheme()))
                || (port == 80 && "http".equalsIgnoreCase(uri.getScheme()))) {
            return host;
        }
        return host + ":" + port;
    }

    /**
     * 执行 encodeObjectKey 流程，围绕 encode object key 完成校验、计算或结果组装。
     *
     * @param objectKey 业务键，用于在同一租户下定位资源。
     * @return 返回 encode object key 生成的文本或业务键。
     */
    private static String encodeObjectKey(String objectKey) {
        String[] segments = objectKey.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodeSegment(segments[i]));
        }
        return encoded.toString();
    }

    /**
     * 执行 encodeSegment 流程，围绕 encode segment 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 encode segment 生成的文本或业务键。
     */
    private static String encodeSegment(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(String.format(Locale.ROOT, "%02X", c));
            }
        }
        return encoded.toString();
    }

    /**
     * 执行 sha256Hex 流程，围绕 sha256 hex 完成校验、计算或结果组装。
     *
     * @param bytes bytes 参数，用于 sha256Hex 流程中的校验、计算或对象转换。
     * @return 返回 sha256 hex 生成的文本或业务键。
     */
    private static String sha256Hex(byte[] bytes) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * 执行 hmac 流程，围绕 hmac 完成校验、计算或结果组装。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 hmac 流程生成的业务结果。
     */
    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("S3 signature failed", e);
        }
    }
}
