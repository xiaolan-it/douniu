package com.douniu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对局详情实体
 */
@Data
@TableName("game_detail")
public class GameDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long gameRecordId;
    private Long userId;
    private Integer seatNumber;
    private Integer betAmount; // 投注额
    private String cards; // 牌面JSON
    private String cardType; // 牌型
    private Integer multiplier; // 赔率
    private Integer scoreChange; // 积分变化
    private Integer isWinner; // 0-否，1-是
    
    @TableField(exist = false) // 非数据库字段
    private String nickname; // 用户昵称（用于显示）
    
    @TableField(exist = false) // 非数据库字段
    private Boolean isDealer; // 是否是庄家（用于显示）
}

