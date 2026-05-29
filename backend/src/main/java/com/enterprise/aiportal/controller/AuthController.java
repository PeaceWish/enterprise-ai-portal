package com.enterprise.aiportal.controller;

import com.enterprise.aiportal.dto.LoginDTO;
import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.UserMapper;
import com.enterprise.aiportal.service.AuthService;
import com.enterprise.aiportal.service.OaClientService;
import com.enterprise.aiportal.util.JwtUtil;
import com.enterprise.aiportal.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final OaClientService oaClientService;
    private final UserMapper userMapper;

    @Value("${oa.sso.client-id:}")
    private String oaClientId;

    public AuthController(AuthService authService, JwtUtil jwtUtil,
                          OaClientService oaClientService, UserMapper userMapper) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.oaClientService = oaClientService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/user-info")
    public Result<LoginVO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.ok(authService.getUserInfo(userId));
    }

    @GetMapping("/refresh")
    public Result<LoginVO> refreshToken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.ok(authService.getUserInfo(userId));
    }

    /**
     * OA SSO 单点登录回调
     * OA 重定向到: /api/auth/sso/callback?ticket=加密账号
     */
    @GetMapping("/sso/callback")
    public Result<LoginVO> ssoCallback(@RequestParam String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Result.error(400, "缺少 ticket 参数");
        }
        log.info("SSO 回调收到 ticket: {}", ticket);

        // 1. 调用 OA 接口解密 ticket
        String username;
        try {
            username = oaClientService.decryptTicket(ticket);
        } catch (Exception e) {
            log.error("SSO 解密失败: {}", e.getMessage());
            return Result.error(401, "单点登录验证失败: " + e.getMessage());
        }

        // 2. 查找本地用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            // 如果本地不存在，尝试用 user_code 查找
            user = userMapper.selectByUserCode(username);
        }

        if (user == null) {
            log.warn("SSO 登录失败: 用户 {} 不存在于本地系统", username);
            return Result.error(401, "用户未同步到本系统，请联系管理员");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("SSO 登录失败: 用户 {} 已被禁用", username);
            return Result.error(401, "用户已被禁用");
        }

        // 3. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 返回用户信息
        LoginVO vo = authService.getUserInfo(user.getId());
        vo.setToken(token);
        vo.setTokenType("Bearer");

        log.info("SSO 登录成功: user={}, username={}", user.getId(), user.getUsername());
        return Result.ok(vo);
    }
}
