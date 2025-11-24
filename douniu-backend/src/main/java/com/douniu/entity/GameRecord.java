package com.douniu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对局记录实体
 */
@Data
@TableName("game_record")
public class GameRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long roomId;
    private Integer roundNumber;
    private Long dealerId;
    private Integer status; // 0-进行中，1-已结算
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

