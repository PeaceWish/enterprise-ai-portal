package com.enterprise.aiportal.security;

import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.SysPermissionMapper;
import com.enterprise.aiportal.mapper.UserMapper;
import com.enterprise.aiportal.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper, SysPermissionMapper sysPermissionMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validate(token)) {
            Long userId = jwtUtil.getUserId(token);
            User user = userMapper.selectById(userId);

            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                // 查询权限编码列表
                List<String> permCodes = sysPermissionMapper.selectPermCodesByUserId(userId);
                List<SimpleGrantedAuthority> authorities = permCodes.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 同时添加 ROLE_ 前缀的角色权限
                if (user.getRole() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
