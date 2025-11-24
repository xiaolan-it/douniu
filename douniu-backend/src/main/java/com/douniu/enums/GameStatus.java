package com.douniu.enums;

import lombok.Getter;

/**
 * 游戏状态枚举
 */
@Getter
public enum GameStatus {
    WAITING(0, "等待中"),
    GAMING(1, "游戏中"),
    FINISHED(2, "已结束");

    private final int code;
    private final String desc;

    GameStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GameStatus fromCode(int code) {
        for (GameStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return WAITING;
    }
}

