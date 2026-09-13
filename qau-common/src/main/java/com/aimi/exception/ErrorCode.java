package com.aimi.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_INCORRECT(1004, "密码错误"),

    // 会话模块 11xx
    SESSION_NOT_FOUND(1101, "会话不存在"),
    MESSAGE_SEND_FAILED(1102, "消息发送失败"),

    // 知识库模块 12xx
    KB_NOT_FOUND(1201, "知识库不存在"),
    KB_SYNC_FAILED(1202, "知识库同步失败"),
    DOCUMENT_PARSE_FAILED(1203, "文档解析失败"),

    // AI 模块 13xx
    AI_SERVICE_UNAVAILABLE(1301, "AI 服务不可用"),
    AI_RATE_LIMIT(1302, "AI 调用频率超限");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
