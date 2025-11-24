package com.douniu.controller;

import com.douniu.dto.ApiResponse;
import com.douniu.dto.CreateRoomRequest;
import com.douniu.entity.Room;
import com.douniu.entity.RoomPlayer;
import com.douniu.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 创建房间
     */
    @PostMapping("/create")
    public ApiResponse<Room> createRoom(@RequestBody CreateRoomRequest request) {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            Room room = roomService.createRoom(userId, request);
            return ApiResponse.success(room);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 根据房间号获取房间信息
     */
    @GetMapping("/code/{roomCode}")
    public ApiResponse<Room> getRoomByCode(@PathVariable String roomCode) {
        try {
            Room room = roomService.getRoomByCode(roomCode);
            if (room == null) {
                return ApiResponse.error("房间不存在");
            }
            return ApiResponse.success(room);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取房间内所有玩家
     */
    @GetMapping("/{roomId}/players")
    public ApiResponse<List<RoomPlayer>> getRoomPlayers(@PathVariable Long roomId) {
        try {
            List<RoomPlayer> players = roomService.getRoomPlayers(roomId);
            return ApiResponse.success(players);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询所有可加入的房间（未开始或进行中）
     */
    @GetMapping("/available")
    public ApiResponse<List<Room>> getAvailableRooms() {
        try {
            List<Room> rooms = roomService.getAvailableRooms();
            return ApiResponse.success(rooms);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

