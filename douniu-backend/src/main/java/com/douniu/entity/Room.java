package com.douniu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房间实体
 */
@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String roomCode; // UUID去掉横杆
    private Long creatorId;
    private Long adminId;
    private Integer maxRounds;
    private Integer currentRound;
    private String enabledCardTypes; // JSON数组，启用的牌型
    private Integer status; // 0-等待中，1-游戏中，2-已结束
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableField(exist = false) // 标记为非数据库字段
    private String creatorNickname; // 创建者昵称（用于显示）
    
    @TableField(exist = false) // 标记为非数据库字段
    private Integer playerCount; // 当前玩家数量（用于显示）
    
    @TableField(exist = false) // 标记为非数据库字段
    private Integer totalProfit; // 用户在该房间的总盈利（用于显示）
}

