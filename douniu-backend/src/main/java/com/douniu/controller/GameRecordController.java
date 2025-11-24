package com.douniu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.douniu.dto.ApiResponse;
import com.douniu.entity.GameDetail;
import com.douniu.entity.GameRecord;
import com.douniu.entity.Room;
import com.douniu.mapper.GameDetailMapper;
import com.douniu.mapper.GameRecordMapper;
import com.douniu.mapper.RoomMapper;
import com.douniu.mapper.RoomPlayerMapper;
import com.douniu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/game-record")
@RequiredArgsConstructor
public class GameRecordController {

    private final GameRecordMapper gameRecordMapper;
    private final GameDetailMapper gameDetailMapper;
    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final UserService userService;

    /**
     * 获取用户参与的房间列表
     */
    @GetMapping("/user/rooms")
    public ApiResponse<List<Room>> getUserRooms() {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            
            // 查询用户参与的房间（通过room_player表）
            LambdaQueryWrapper<com.douniu.entity.RoomPlayer> playerWrapper = new LambdaQueryWrapper<>();
            playerWrapper.eq(com.douniu.entity.RoomPlayer::getUserId, userId)
                    .select(com.douniu.entity.RoomPlayer::getRoomId)
                    .groupBy(com.douniu.entity.RoomPlayer::getRoomId);
            List<com.douniu.entity.RoomPlayer> players = roomPlayerMapper.selectList(playerWrapper);
            
            if (players.isEmpty()) {
                return ApiResponse.success(List.of());
            }
            
            List<Long> roomIds = players.stream()
                    .map(com.douniu.entity.RoomPlayer::getRoomId)
                    .distinct()
                    .collect(Collectors.toList());
            
            LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Room::getId, roomIds)
                    .orderByDesc(Room::getCreatedAt);
            List<Room> rooms = roomMapper.selectList(wrapper);
            
            // 填充创建者信息、参与人数和总盈利
            for (Room room : rooms) {
                // 填充创建者昵称
                if (room.getCreatorId() != null) {
                    com.douniu.entity.User creator = userService.getUserById(room.getCreatorId());
                    if (creator != null) {
                        room.setCreatorNickname(creator.getNickname());
                    }
                }
                
                // 填充当前玩家数量
                LambdaQueryWrapper<com.douniu.entity.RoomPlayer> playerCountWrapper = new LambdaQueryWrapper<>();
                playerCountWrapper.eq(com.douniu.entity.RoomPlayer::getRoomId, room.getId());
                long count = roomPlayerMapper.selectCount(playerCountWrapper);
                room.setPlayerCount((int) count);
                
                // 计算用户在该房间的总盈利（统计该用户在该房间所有已结算对局的积分变化总和）
                LambdaQueryWrapper<GameRecord> recordWrapper = new LambdaQueryWrapper<>();
                recordWrapper.eq(GameRecord::getRoomId, room.getId())
                        .eq(GameRecord::getStatus, 1); // 只统计已结算的对局（status=1表示已结算）
                List<GameRecord> roomRecords = gameRecordMapper.selectList(recordWrapper);
                
                int totalProfit = 0;
                for (GameRecord record : roomRecords) {
                    // 查询用户在该对局中的详情
                    LambdaQueryWrapper<GameDetail> detailWrapper = new LambdaQueryWrapper<>();
                    detailWrapper.eq(GameDetail::getGameRecordId, record.getId())
                            .eq(GameDetail::getUserId, userId);
                    // 使用 selectList 然后取第一条，避免 TooManyResultsException
                    List<GameDetail> userDetails = gameDetailMapper.selectList(detailWrapper);
                    if (!userDetails.isEmpty()) {
                        GameDetail userDetail = userDetails.get(0);
                        if (userDetail.getScoreChange() != null) {
                            totalProfit += userDetail.getScoreChange();
                        }
                    }
                }
                room.setTotalProfit(totalProfit);
            }
            
            return ApiResponse.success(rooms);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取房间的对局记录列表（包含用户在该对局中的收支和牌型）
     */
    @GetMapping("/room/{roomId}")
    public ApiResponse<List<Map<String, Object>>> getRoomRecords(@PathVariable Long roomId) {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            
            LambdaQueryWrapper<GameRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameRecord::getRoomId, roomId)
                    .orderByAsc(GameRecord::getRoundNumber); // 按局数升序
            List<GameRecord> records = gameRecordMapper.selectList(wrapper);
            
            // 为每个对局记录添加用户的收支和牌型信息
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (GameRecord record : records) {
                Map<String, Object> recordData = new HashMap<>();
                recordData.put("id", record.getId());
                recordData.put("roomId", record.getRoomId());
                recordData.put("roundNumber", record.getRoundNumber());
                recordData.put("startTime", record.getStartTime());
                recordData.put("endTime", record.getEndTime());
                recordData.put("status", record.getStatus());
                
                // 查询用户在该对局中的详情
                LambdaQueryWrapper<GameDetail> detailWrapper = new LambdaQueryWrapper<>();
                detailWrapper.eq(GameDetail::getGameRecordId, record.getId())
                        .eq(GameDetail::getUserId, userId);
                // 使用 selectList 然后取第一条，避免 TooManyResultsException
                List<GameDetail> userDetails = gameDetailMapper.selectList(detailWrapper);
                
                if (!userDetails.isEmpty()) {
                    GameDetail userDetail = userDetails.get(0);
                    recordData.put("scoreChange", userDetail.getScoreChange());
                    recordData.put("cardType", userDetail.getCardType());
                    recordData.put("betAmount", userDetail.getBetAmount());
                } else {
                    recordData.put("scoreChange", 0);
                    recordData.put("cardType", null);
                    recordData.put("betAmount", 0);
                }
                
                result.add(recordData);
            }
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取对局详情（包含用户昵称和庄家信息）
     */
    @GetMapping("/{gameRecordId}/details")
    public ApiResponse<List<GameDetail>> getGameDetails(@PathVariable Long gameRecordId) {
        try {
            LambdaQueryWrapper<GameDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameDetail::getGameRecordId, gameRecordId)
                    .orderByAsc(GameDetail::getSeatNumber);
            List<GameDetail> details = gameDetailMapper.selectList(wrapper);
            
            // 获取对局记录以获取庄家ID
            GameRecord record = gameRecordMapper.selectById(gameRecordId);
            Long dealerId = record != null ? record.getDealerId() : null;
            
            // 填充用户昵称和庄家信息
            for (GameDetail detail : details) {
                // 填充用户昵称
                if (detail.getUserId() != null) {
                    com.douniu.entity.User user = userService.getUserById(detail.getUserId());
                    if (user != null) {
                        detail.setNickname(user.getNickname());
                    }
                }
                
                // 判断是否是庄家
                detail.setIsDealer(dealerId != null && dealerId.equals(detail.getUserId()));
            }
            
            return ApiResponse.success(details);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的对局记录
     */
    @GetMapping("/user")
    public ApiResponse<List<GameRecord>> getUserRecords() {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            
            // 查询用户参与的对局（通过game_detail表关联）
            LambdaQueryWrapper<GameDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(GameDetail::getUserId, userId)
                    .select(GameDetail::getGameRecordId)
                    .groupBy(GameDetail::getGameRecordId);
            List<GameDetail> details = gameDetailMapper.selectList(detailWrapper);
            
            if (details.isEmpty()) {
                return ApiResponse.success(List.of());
            }
            
            List<Long> recordIds = details.stream()
                    .map(GameDetail::getGameRecordId)
                    .distinct()
                    .toList();
            
            LambdaQueryWrapper<GameRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(GameRecord::getId, recordIds)
                    .orderByDesc(GameRecord::getStartTime);
            List<GameRecord> records = gameRecordMapper.selectList(wrapper);
            
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

