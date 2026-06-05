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

    @Override
    public boolean configured() {
        return enabled && hasText(host) && hasText(defaultFrom);
    }

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
        } catch (IOException e) {
            throw new IllegalStateException("SMTP email delivery failed: " + e.getMessage(), e);
        }
    }

    private Socket openSocket() throws IOException {
        Socket socket = ssl
                ? SSLSocketFactory.getDefault().createSocket()
                : new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        return socket;
    }

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

    private String multipartMessage(String from,
                                    List<String> to,
                                    String subject,
                                    String text,
                                    List<BiEmailAttachment> attachments) {
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
        return builder.toString();
    }

    private void writeData(BufferedWriter writer, String message) throws IOException {
        for (String line : message.split("\\r?\\n", -1)) {
            writer.write(line.startsWith(".") ? "." + line : line);
            writer.write("\r\n");
        }
        writer.write(".\r\n");
        writer.flush();
    }

    private void command(BufferedWriter writer,
                         BufferedReader reader,
                         String command,
                         int... expectedCodes) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        expect(reader, expectedCodes);
    }

    private void expect(BufferedReader reader, int... expectedCodes) throws IOException {
        SmtpResponse response = readResponse(reader);
        for (int expectedCode : expectedCodes) {
            if (response.code() == expectedCode) {
                return;
            }
        }
        throw new IOException("unexpected SMTP response " + response.code() + ": " + response.message());
    }

    private SmtpResponse readResponse(BufferedReader reader) throws IOException {
        StringBuilder message = new StringBuilder();
        int code = -1;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("SMTP server closed connection");
            }
            if (line.length() >= 3) {
                code = Integer.parseInt(line.substring(0, 3));
            }
            message.append(line).append('\n');
            if (line.length() < 4 || line.charAt(3) != '-') {
                return new SmtpResponse(code, message.toString());
            }
        }
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private String headerEscape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SmtpResponse(int code, String message) {
    }
}
