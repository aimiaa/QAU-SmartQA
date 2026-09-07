package com.aimi.security;

import com.aimi.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 Authorization 请求头解析 Bearer 令牌并写入 SecurityContext。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtTokenProvider.parseToken(token);
                if (claims != null) {
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    var authorities = role == null
                            ? List.<SimpleGrantedAuthority>of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    UserContext.setUserId(resolveUserId(claims));
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private Long resolveUserId(Claims claims) {
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        if (uid != null) {
            try {
                return Long.valueOf(uid.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (StringUtils.hasText(prefix) && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }
}
