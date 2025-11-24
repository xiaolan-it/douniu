package com.douniu.controller;

import com.douniu.dto.ApiResponse;
import com.douniu.dto.LoginRequest;
import com.douniu.dto.RegisterRequest;
import com.douniu.entity.User;
import com.douniu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            // 不返回密码
            user.setPassword(null);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<com.douniu.dto.LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request);
            // 不返回密码
            user.setPassword(null);
            
            // 获取token
            String token = cn.dev33.satoken.stp.StpUtil.getTokenValue();
            
            // 返回用户信息和token
            com.douniu.dto.LoginResponse response = new com.douniu.dto.LoginResponse(user, token);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            User user = userService.getUserById(userId);
            if (user != null) {
                user.setPassword(null);
            }
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error("未登录");
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        cn.dev33.satoken.stp.StpUtil.logout();
        return ApiResponse.success(null);
    }
}

