package com.douniu.enums;

import lombok.Getter;

/**
 * 对局状态枚举
 */
@Getter
public enum RoundStatus {
    IN_PROGRESS(0, "进行中"),
    SETTLED(1, "已结算");

    private final int code;
    private final String desc;

    RoundStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RoundStatus fromCode(int code) {
        for (RoundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return IN_PROGRESS;
    }
}

