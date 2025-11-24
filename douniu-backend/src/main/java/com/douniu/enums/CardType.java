package com.douniu.enums;

import lombok.Getter;

/**
 * 牌型枚举
 */
@Getter
public enum CardType {
    WU_XIAO_NIU("五小牛", 5),
    ZHA_DAN_NIU("炸弹牛", 4),
    WU_HUA_NIU("五花牛", 4),
    SHUN_ZI_NIU("顺子", 4),
    NIU_NIU("牛牛", 3),
    NIU_9("牛9", 2),
    NIU_8("牛8", 2),
    NIU_7("牛7", 1),
    NIU_6("牛6", 1),
    NIU_5("牛5", 1),
    NIU_4("牛4", 1),
    NIU_3("牛3", 1),
    NIU_2("牛2", 1),
    NIU_1("牛1", 1),
    WU_NIU("无牛", 1);

    private final String name;
    private final int multiplier;

    CardType(String name, int multiplier) {
        this.name = name;
        this.multiplier = multiplier;
    }

    public static CardType fromName(String name) {
        for (CardType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}

