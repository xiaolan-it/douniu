package com.douniu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Sa-Token Redis配置
 * 
 * 当引入了 sa-token-redis-jackson 依赖后，Sa-Token 会自动检测并使用 Redis
 * 此配置类用于确保 Redis 配置正确，Sa-Token 会自动使用 Redis 存储 token
 * 
 * 如果 token 在重启后失效，请检查：
 * 1. Redis 服务是否正常运行
 * 2. Redis 连接配置是否正确（host、port、password）
 * 3. 启动日志中是否有 Sa-Token 使用 Redis 的提示
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class SaTokenRedisConfig {
    // Sa-Token 会自动检测 Redis 并使用它，无需手动配置
    // 如果自动配置失败，请检查 Redis 连接和依赖
}

