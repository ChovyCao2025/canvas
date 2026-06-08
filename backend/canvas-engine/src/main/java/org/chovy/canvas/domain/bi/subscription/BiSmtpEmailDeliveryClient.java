package org.chovy.canvas.domain.bi.subscription;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * BiSmtpEmailDeliveryClient 编排 domain.bi.subscription 场景的领域业务规则。
 */
@Service
public class BiSmtpEmailDeliveryClient implements BiEmailDeliveryClient {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String defaultFrom;
    private final boolean ssl;
    private final boolean startTls;
    private final int timeoutMs;

    /**
     * 创建 BiSmtpEmailDeliveryClient 实例并注入 domain.bi.subscription 场景依赖。
     * @param enabled enabled 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param host host 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param port port 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param password password 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param defaultFrom 时间或范围边界，用于限定统计窗口。
     * @param ssl ssl 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param startTls start tls 参数，用于 BiSmtpEmailDeliveryClient 流程中的校验、计算或对象转换。
     * @param timeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    public BiSmtpEmailDeliveryClient(
            @Value("${canvas.bi.delivery.email.enabled:false}") boolean enabled,
            @Value("${canvas.bi.delivery.email.host:}") String host,
            @Value("${canvas.bi.delivery.email.port:25}") int port,
            @Value("${canvas.bi.delivery.email.username:}") String username,
            @Value("${canvas.bi.delivery.email.password:}") String password,
            @Value("${canvas.bi.delivery.email.from:}") String defaultFrom,
            @Value("${canvas.bi.delivery.email.ssl:false}") boolean ssl,
            @Value("${canvas.bi.delivery.email.starttls:false}") boolean startTls,
            @Value("${canvas.bi.delivery.email.timeout-ms:10000}") int timeoutMs) {
        this.enabled = enabled;
        this.host = text(host);
        this.port = port;
        this.username = text(username);
        this.password = password == null ? "" : password;
        this.defaultFrom = text(defaultFrom);
        this.ssl = ssl;
        this.startTls = startTls;
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    /**
     * 判断 SMTP 邮件投递客户端是否具备最小发送配置。
     *
     * @return {@code true} 表示邮件开关启用，且已配置 SMTP 主机和默认发件人
     */
    @Override
    public boolean configured() {
        return enabled && hasText(host) && hasText(defaultFrom);
    }

    /**
     * 通过 SMTP 协议发送 BI 订阅邮件。
     *
     * <p>方法会建立明文、SSL 或 STARTTLS 连接，按需执行 AUTH LOGIN，逐个写入收件人，并发送文本或带附件的 MIME 邮件。
     * 发送成功无返回；配置缺失、收件人为空或 SMTP 会话失败会抛出异常。</p>
     *
     * @param request 邮件投递请求，包含发件人、收件人、主题、正文和附件
     */
    @Override
    public void send(BiEmailDeliveryRequest request) {
        if (!configured()) {
            throw new IllegalStateException("SMTP email adapter is not configured");
        }
        if (request.to().isEmpty()) {
            throw new IllegalArgumentException("email recipients are required");
        }
        String from = hasText(request.from()) ? request.from() : defaultFrom;
        try {
            Socket socket = openSocket();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            expect(reader, 220);
            command(writer, reader, "EHLO canvas-bi", 250);
            if (startTls && !ssl) {
                command(writer, reader, "STARTTLS", 220);
                Socket tlsSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .createSocket(socket, host, port, true);
                tlsSocket.setSoTimeout(timeoutMs);
                try (tlsSocket;
                     BufferedReader tlsReader = new BufferedReader(new InputStreamReader(tlsSocket.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter tlsWriter = new BufferedWriter(new OutputStreamWriter(tlsSocket.getOutputStream(), StandardCharsets.UTF_8))) {
                    command(tlsWriter, tlsReader, "EHLO canvas-bi", 250);
                    smtpConversation(tlsWriter, tlsReader, from, request);
                }
                return;
            }
            try (socket; reader; writer) {
                smtpConversation(writer, reader, from, request);
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IOException e) {
            throw new IllegalStateException("SMTP email delivery failed: " + e.getMessage(), e);
        }
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @return 返回 openSocket 流程生成的业务结果。
     */
    private Socket openSocket() throws IOException {
        Socket socket = ssl
                ? SSLSocketFactory.getDefault().createSocket()
                /**
                 * 执行 Socket 流程，围绕 socket 完成校验、计算或结果组装。
                 *
                 * @return 返回 Socket 流程生成的业务结果。
                 */
                : new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        return socket;
    }

    /**
     * 执行 smtpConversation 流程，围绕 smtp conversation 完成校验、计算或结果组装。
     *
     * @param writer writer 参数，用于 smtpConversation 流程中的校验、计算或对象转换。
     * @param reader reader 参数，用于 smtpConversation 流程中的校验、计算或对象转换。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void smtpConversation(BufferedWriter writer,
                                  BufferedReader reader,
                                  String from,
                                  BiEmailDeliveryRequest request) throws IOException {
        if (hasText(username)) {
            command(writer, reader, "AUTH LOGIN", 334);
            command(writer, reader, base64(username), 334);
            command(writer, reader, base64(password), 235);
        }
        command(writer, reader, "MAIL FROM:<" + from + ">", 250);
        for (String recipient : request.to()) {
            command(writer, reader, "RCPT TO:<" + recipient + ">", 250, 251);
        }
        command(writer, reader, "DATA", 354);
        writeData(writer, message(from, request));
        expect(reader, 250);
        command(writer, reader, "QUIT", 221);
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(String from, BiEmailDeliveryRequest request) {
        List<String> to = request.to();
        String subject = hasText(request.subject()) ? request.subject() : "BI 通知";
        String text = request.text() == null ? "" : request.text();
        if (!request.attachments().isEmpty()) {
            return multipartMessage(from, to, subject, text, request.attachments());
        }
        return "From: " + from + "\r\n"
                + "To: " + String.join(",", to) + "\r\n"
                + "Subject: =?UTF-8?B?" + base64(subject) + "?=\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Date: " + LocalDateTime.now() + "\r\n"
                + "\r\n"
                + text + "\r\n";
    }

    /**
     * 执行 multipartMessage 流程，围绕 multipart message 完成校验、计算或结果组装。
     *
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param subject 待处理业务值，用于规则计算、转换或外部调用。
     * @param text text 参数，用于 multipartMessage 流程中的校验、计算或对象转换。
     * @param attachments attachments 参数，用于 multipartMessage 流程中的校验、计算或对象转换。
     * @return 返回 multipart message 生成的文本或业务键。
     */
    private String multipartMessage(String from,
                                    List<String> to,
                                    String subject,
                                    String text,
                                    List<BiEmailAttachment> attachments) {
        // 准备本次处理所需的上下文和中间变量。
        String boundary = "canvas-bi-" + UUID.randomUUID();
        StringBuilder builder = new StringBuilder()
                .append("From: ").append(from).append("\r\n")
                .append("To: ").append(String.join(",", to)).append("\r\n")
                .append("Subject: =?UTF-8?B?").append(base64(subject)).append("?=\r\n")
                .append("MIME-Version: 1.0\r\n")
                .append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n")
                .append("Date: ").append(LocalDateTime.now()).append("\r\n")
                .append("\r\n")
                .append("--").append(boundary).append("\r\n")
                .append("Content-Type: text/plain; charset=UTF-8\r\n")
                .append("Content-Transfer-Encoding: 8bit\r\n")
                .append("\r\n")
                .append(text).append("\r\n");
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiEmailAttachment attachment : attachments) {
            if (attachment.bytes().length == 0) {
                continue;
            }
            String filename = hasText(attachment.fileName()) ? attachment.fileName() : "attachment.bin";
            String contentType = hasText(attachment.contentType()) ? attachment.contentType() : "application/octet-stream";
            builder.append("--").append(boundary).append("\r\n")
                    .append("Content-Type: ").append(contentType)
                    .append("; name=\"").append(headerEscape(filename)).append("\"\r\n")
                    .append("Content-Transfer-Encoding: base64\r\n")
                    .append("Content-Disposition: attachment; filename=\"").append(headerEscape(filename)).append("\"\r\n")
                    .append("\r\n")
                    .append(Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                            .encodeToString(attachment.bytes()))
                    .append("\r\n");
        }
        builder.append("--").append(boundary).append("--\r\n");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return builder.toString();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param writer writer 参数，用于 writeData 流程中的校验、计算或对象转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private void writeData(BufferedWriter writer, String message) throws IOException {
        for (String line : message.split("\\r?\\n", -1)) {
            writer.write(line.startsWith(".") ? "." + line : line);
            writer.write("\r\n");
        }
        writer.write(".\r\n");
        writer.flush();
    }

    /**
     * 执行 command 流程，围绕 command 完成校验、计算或结果组装。
     *
     * @param writer writer 参数，用于 command 流程中的校验、计算或对象转换。
     * @param reader reader 参数，用于 command 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param expectedCodes expected codes 参数，用于 command 流程中的校验、计算或对象转换。
     */
    private void command(BufferedWriter writer,
                         BufferedReader reader,
                         String command,
                         int... expectedCodes) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        expect(reader, expectedCodes);
    }

    /**
     * 执行 expect 流程，围绕 expect 完成校验、计算或结果组装。
     *
     * @param reader reader 参数，用于 expect 流程中的校验、计算或对象转换。
     * @param expectedCodes expected codes 参数，用于 expect 流程中的校验、计算或对象转换。
     */
    private void expect(BufferedReader reader, int... expectedCodes) throws IOException {
        SmtpResponse response = readResponse(reader);
        for (int expectedCode : expectedCodes) {
            if (response.code() == expectedCode) {
                return;
            }
        }
        throw new IOException("unexpected SMTP response " + response.code() + ": " + response.message());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param reader reader 参数，用于 readResponse 流程中的校验、计算或对象转换。
     * @return 返回 readResponse 流程生成的业务结果。
     */
    private SmtpResponse readResponse(BufferedReader reader) throws IOException {
        StringBuilder message = new StringBuilder();
        int code = -1;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (true) {
            String line = reader.readLine();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (line == null) {
                throw new IOException("SMTP server closed connection");
            }
            if (line.length() >= 3) {
                code = Integer.parseInt(line.substring(0, 3));
            }
            message.append(line).append('\n');
            if (line.length() < 4 || line.charAt(3) != '-') {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return new SmtpResponse(code, message.toString());
            }
        }
    }

    /**
     * 执行 base64 流程，围绕 base64 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 base64 生成的文本或业务键。
     */
    private String base64(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 执行 headerEscape 流程，围绕 header escape 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 header escape 生成的文本或业务键。
     */
    private String headerEscape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * SmtpResponse 数据记录。
     */
    private record SmtpResponse(int code, String message) {
    }
}
