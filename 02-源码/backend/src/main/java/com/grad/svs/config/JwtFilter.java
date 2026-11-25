package com.grad.svs.config;

import com.grad.svs.common.JwtUtil;
import com.grad.svs.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器：解析 Authorization 头，写入当前登录人上下文，请求结束后清理 ThreadLocal。
 * 解析失败不直接返回 401，交由 Security 的入口处理，保证统一响应结构。
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwtUtil.parse(header.substring(7));
                Long userId = Long.valueOf(c.getSubject());
                Long orgId = c.get("orgId") == null ? null : Long.valueOf(String.valueOf(c.get("orgId")));
                String role = String.valueOf(c.get("role"));
                UserContext.set(new UserContext.LoginUser(userId, String.valueOf(c.get("name")),
                        role, orgId, String.valueOf(c.get("scope"))));
                // 同步写入 SecurityContext，供 Spring Security 的 authenticated() 判定使用
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                UserContext.clear();
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
