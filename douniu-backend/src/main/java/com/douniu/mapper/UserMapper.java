package com.douniu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.douniu.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

