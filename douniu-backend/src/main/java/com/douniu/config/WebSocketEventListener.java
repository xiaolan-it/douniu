package com.douniu.config;

import com.douniu.entity.Room;
import com.douniu.entity.RoomPlayer;
import com.douniu.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.douniu.dto.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket事件监听器
 * 用于跟踪玩家的在线状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    
    // 存储用户ID和房间ID的映射（key: userId, value: roomId）
    private final Map<Long, Long> userRoomMap = new ConcurrentHashMap<>();
    
    // 存储会话ID和用户ID的映射（key: sessionId, value: userId）
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * WebSocket连接建立时
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket连接建立: sessionId={}", sessionId);
        
        // 注意：此时可能还没有userId信息，因为用户可能还没有发送joinRoom消息
        // 所以在线状态会在joinRoom时更新
    }

    /**
     * WebSocket连接断开时
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket连接断开: sessionId={}", sessionId);
        
        // 从sessionUserMap获取userId
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            // 从userRoomMap获取roomId并移除
            Long roomId = userRoomMap.remove(userId);
            if (roomId != null) {
                // 广播房间更新，通知其他玩家该玩家已离线
                broadcastRoomUpdate(roomId);
            }
        }
    }
    
    /**
     * 记录用户加入房间（由WebSocketMessageHandler调用）
     */
    public void recordUserJoinRoom(Long userId, Long roomId) {
        recordUserJoinRoom(userId, roomId, null);
    }
    
    /**
     * 记录用户加入房间（带sessionId）
     */
    public void recordUserJoinRoom(Long userId, Long roomId, String sessionId) {
        userRoomMap.put(userId, roomId);
        if (sessionId != null) {
            sessionUserMap.put(sessionId, userId);
        }
        broadcastRoomUpdate(roomId);
    }
    
    /**
     * 记录用户离开房间（由WebSocketMessageHandler调用）
     */
    public void recordUserLeaveRoom(Long userId) {
        Long roomId = userRoomMap.remove(userId);
        if (roomId != null) {
            broadcastRoomUpdate(roomId);
        }
    }
    
    /**
     * 获取用户是否在线（根据是否有活跃的WebSocket连接）
     */
    public boolean isUserOnline(Long userId) {
        return userRoomMap.containsKey(userId);
    }
    
    /**
     * 广播房间更新
     */
    private void broadcastRoomUpdate(Long roomId) {
        Room room = roomService.getRoom(roomId);
        // 获取玩家列表，并填充在线状态
        List<RoomPlayer> players = roomService.getRoomPlayers(roomId, this::isUserOnline);

        Map<String, Object> data = new HashMap<>();
        data.put("room", room);
        data.put("players", players);

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/update",
                ApiResponse.success(data));
    }
}

