package com.douniu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    private Integer balance; // 全局积分余额
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

