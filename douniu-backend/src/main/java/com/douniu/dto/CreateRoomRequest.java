package com.douniu.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateRoomRequest {
    private Integer maxRounds; // 总局数
    private List<String> enabledCardTypes; // 启用的牌型列表
}

