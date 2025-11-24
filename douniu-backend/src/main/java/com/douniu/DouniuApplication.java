package com.douniu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.douniu.mapper")
public class DouniuApplication {
    public static void main(String[] args) {
        SpringApplication.run(DouniuApplication.class, args);
    }
}

