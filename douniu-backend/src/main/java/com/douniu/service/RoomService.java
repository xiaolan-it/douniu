package com.douniu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.douniu.dto.CreateRoomRequest;
import com.douniu.entity.Room;
import com.douniu.entity.RoomPlayer;
import com.douniu.entity.User;
import com.douniu.enums.GameStatus;
import com.douniu.mapper.RoomMapper;
import com.douniu.mapper.RoomPlayerMapper;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final UserService userService;

    /**
     * 创建房间
     */
    @Transactional
    public Room createRoom(Long creatorId, CreateRoomRequest request) {
        Room room = new Room();
        room.setRoomCode(UUID.randomUUID().toString().replace("-", ""));
        room.setCreatorId(creatorId);
        room.setAdminId(creatorId); // 默认创建者为管理员
        room.setMaxRounds(request.getMaxRounds() != null ? request.getMaxRounds() : 20);
        room.setCurrentRound(0);
        
        // 必选牌型（始终包含）
        List<String> requiredTypes = Arrays.asList("无牛", "牛1", "牛2", "牛3", "牛4", "牛5", "牛6", "牛7", "牛8", "牛9", "牛牛");
        
        // 合并必选牌型和用户选择的可选牌型
        List<String> enabledTypes = new ArrayList<>(requiredTypes);
        if (request.getEnabledCardTypes() != null) {
            for (String type : request.getEnabledCardTypes()) {
                if (!requiredTypes.contains(type)) {
                    enabledTypes.add(type);
                }
            }
        }
        
        room.setEnabledCardTypes(JSON.toJSONString(enabledTypes));
        room.setStatus(GameStatus.WAITING.getCode());

        roomMapper.insert(room);

        // 创建者自动加入房间，并设置为庄家
        RoomPlayer creatorPlayer = joinRoom(room.getId(), creatorId, 1);
        creatorPlayer.setIsDealer(1); // 创建者默认为庄家
        roomPlayerMapper.updateById(creatorPlayer);

        return room;
    }

    /**
     * 加入房间
     */
    @Transactional
    public RoomPlayer joinRoom(Long roomId, Long userId, Integer seatNumber) {
        // 检查房间是否存在
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        // 检查房间是否已满（最多10人）
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId);
        long playerCount = roomPlayerMapper.selectCount(wrapper);
        if (playerCount >= 10) {
            throw new RuntimeException("房间已满");
        }

        // 检查用户是否已在房间
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getUserId, userId);
        if (roomPlayerMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("已在房间中");
        }

        // 如果未指定座位号，自动分配
        if (seatNumber == null) {
            seatNumber = findAvailableSeat(roomId);
        }

        // 检查座位是否被占用
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getSeatNumber, seatNumber);
        if (roomPlayerMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("座位已被占用");
        }

        RoomPlayer roomPlayer = new RoomPlayer();
        roomPlayer.setRoomId(roomId);
        roomPlayer.setUserId(userId);
        roomPlayer.setSeatNumber(seatNumber);
        roomPlayer.setIsDealer(0);
        roomPlayer.setTotalScore(0);

        roomPlayerMapper.insert(roomPlayer);
        return roomPlayer;
    }

    /**
     * 查找可用座位
     */
    private Integer findAvailableSeat(Long roomId) {
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .select(RoomPlayer::getSeatNumber);
        List<RoomPlayer> players = roomPlayerMapper.selectList(wrapper);
        
        boolean[] seats = new boolean[11]; // 1-10
        for (RoomPlayer player : players) {
            seats[player.getSeatNumber()] = true;
        }

        for (int i = 1; i <= 10; i++) {
            if (!seats[i]) {
                return i;
            }
        }
        throw new RuntimeException("房间已满");
    }

    /**
     * 根据房间号获取房间
     */
    public Room getRoomByCode(String roomCode) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getRoomCode, roomCode);
        return roomMapper.selectOne(wrapper);
    }

    /**
     * 获取房间内所有玩家
     */
    public List<RoomPlayer> getRoomPlayers(Long roomId) {
        return getRoomPlayers(roomId, null);
    }
    
    /**
     * 获取房间内所有玩家（带在线状态）
     */
    public List<RoomPlayer> getRoomPlayers(Long roomId, java.util.function.Function<Long, Boolean> onlineStatusChecker) {
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .orderByAsc(RoomPlayer::getSeatNumber);
        List<RoomPlayer> players = roomPlayerMapper.selectList(wrapper);
        
        // 填充用户昵称和在线状态
        for (RoomPlayer player : players) {
            User user = userService.getUserById(player.getUserId());
            if (user != null) {
                player.setNickname(user.getNickname());
            }
            
            // 填充在线状态
            if (onlineStatusChecker != null) {
                player.setIsOnline(onlineStatusChecker.apply(player.getUserId()));
            } else {
                // 默认在线（如果没有提供检查器）
                player.setIsOnline(true);
            }
        }
        
        return players;
    }

    /**
     * 设置管理员
     */
    @Transactional
    public void setAdmin(Long roomId, Long adminId, Long newAdminId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        // 检查操作者是否为当前管理员
        if (!room.getAdminId().equals(adminId)) {
            throw new RuntimeException("无权限");
        }

        // 检查新管理员是否在房间中
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getUserId, newAdminId);
        if (roomPlayerMapper.selectOne(wrapper) == null) {
            throw new RuntimeException("用户不在房间中");
        }

        room.setAdminId(newAdminId);
        roomMapper.updateById(room);
    }

    /**
     * 离开房间
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        LambdaQueryWrapper<RoomPlayer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomPlayer::getRoomId, roomId)
                .eq(RoomPlayer::getUserId, userId);
        roomPlayerMapper.delete(wrapper);
    }

    /**
     * 根据ID获取房间
     */
    public Room getRoom(Long roomId) {
        return roomMapper.selectById(roomId);
    }

    /**
     * 查询所有未开始或进行中的房间
     */
    public List<Room> getAvailableRooms() {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        // 查询状态为等待中(0)或游戏中(1)的房间
        wrapper.in(Room::getStatus, GameStatus.WAITING.getCode(), GameStatus.GAMING.getCode())
                .orderByDesc(Room::getCreatedAt);
        List<Room> rooms = roomMapper.selectList(wrapper);
        
        // 填充创建者信息和玩家数量
        for (Room room : rooms) {
            // 填充创建者昵称
            if (room.getCreatorId() != null) {
                User creator = userService.getUserById(room.getCreatorId());
                if (creator != null) {
                    room.setCreatorNickname(creator.getNickname());
                }
            }
            
            // 填充当前玩家数量
            LambdaQueryWrapper<RoomPlayer> playerWrapper = new LambdaQueryWrapper<>();
            playerWrapper.eq(RoomPlayer::getRoomId, room.getId());
            long count = roomPlayerMapper.selectCount(playerWrapper);
            room.setPlayerCount((int) count);
        }
        
        return rooms;
    }
}

