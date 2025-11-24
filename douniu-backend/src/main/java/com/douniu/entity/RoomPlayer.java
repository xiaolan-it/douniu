package com.douniu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房间玩家实体
 */
@Data
@TableName("room_player")
public class RoomPlayer {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long roomId;
    private Long userId;
    private Integer seatNumber; // 座位号 1-10
    private Integer isDealer; // 0-否，1-是
    private Integer totalScore; // 当前房间总积分（每局开始时重置为0）
    private LocalDateTime joinedAt;
    
    // 非数据库字段，用于返回用户昵称
    @TableField(exist = false)
    private String nickname;
    
    // 非数据库字段，用于返回在线状态
    @TableField(exist = false)
    private Boolean isOnline; // true-在线，false-离线
}

