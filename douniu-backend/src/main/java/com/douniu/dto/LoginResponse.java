package com.douniu.dto;

import com.douniu.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private User user;
    private String token;

    public LoginResponse(User user, String token) {
        this.user = user;
        this.token = token;
    }
}

