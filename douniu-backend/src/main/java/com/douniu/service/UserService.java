package com.douniu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.douniu.dto.LoginRequest;
import com.douniu.dto.RegisterRequest;
import com.douniu.entity.User;
import com.douniu.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    public User register(RegisterRequest request) {
        // 检查手机号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getPhone());
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("手机号已注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(7));
        user.setBalance(0);

        userMapper.insert(user);
        return user;
    }

    /**
     * 用户登录
     */
    public User login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, request.getPhone());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // Sa-Token登录
        StpUtil.login(user.getId());
        return user;
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户积分
     */
    public void updateBalance(Long userId, Integer scoreChange) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setBalance(user.getBalance() + scoreChange);
            userMapper.updateById(user);
        }
    }
}

