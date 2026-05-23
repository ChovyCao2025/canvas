package org.chovy.canvas.auth.dto;

import lombok.Data;

/**
 * 登录请求参数。
 *
 * <p>由登录页提交，后端用于鉴权并签发 JWT。
 * 两个字段都为必填。
 * 该对象只在登录接口中使用，不参与 token 刷新流程。
 * 参数格式校验（长度/字符集）建议在 Controller 层配合校验注解补齐。
 */
@Data
public class LoginReq {

    /**
     * 登录用户名。
     *
     * <p>约束建议：
     * - 前端可限制首尾空格；
     * - 后端可统一做 trim 后再比对，避免“视觉一致但字符串不同”。
     */
    private String username;

    /**
     * 登录密码（明文仅在 HTTPS 传输链路中出现，落库前会做加密比对）。
     *
     * <p>安全注意：
     * - 该字段不应被打印到应用日志；
     * - 异常信息不要回显密码格式细节。
     */
    private String password;

    // 该 DTO 不应在日志中直接打印，避免泄露敏感字段。
}
