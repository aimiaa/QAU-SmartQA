package com.aimi.security;

import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;

/**
 * 当前登录用户上下文。基于 ThreadLocal，仅在同步请求线程内有效。
 * 流式/异步场景必须在进入响应式边界前调用 {@link #requireUserId()} 取值并显式传参，
 * 严禁在 Flux 的 onNext/doOnComplete 等回调线程里读取。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前用户 id，缺失时抛出未认证异常。
     */
    public static Long requireUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getMessage());
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}