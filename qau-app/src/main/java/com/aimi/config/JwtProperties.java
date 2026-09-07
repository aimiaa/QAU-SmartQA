package com.aimi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 相关配置，对应 application.yml 中 jwt.* 前缀。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥，HS256 要求至少 32 字节 */
    private String secret;

    /** 令牌有效期（毫秒） */
    private long expiration;

    /** 存放令牌的请求头名称 */
    private String header = "Authorization";

    /** 令牌前缀 */
    private String tokenPrefix = "Bearer ";
}