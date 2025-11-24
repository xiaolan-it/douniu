package com.douniu.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定拦截的路由
            SaRouter.match("/**")
                    .notMatch("/auth/login", "/auth/register", "/ws/**") // 排除登录、注册和WebSocket
                    .check(r -> StpUtil.checkLogin()); // 检查是否登录
        })).addPathPatterns("/**");
    }
}

