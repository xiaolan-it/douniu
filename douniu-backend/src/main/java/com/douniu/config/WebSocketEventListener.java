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
    
    // 存储用户ID和会话ID的映射（key: userId, value: sessionId），一个用户只能有一个会话
    private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();

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
            // 检查是否是当前用户的会话
            String currentSessionId = userSessionMap.get(userId);
            if (sessionId.equals(currentSessionId)) {
                // 这是当前活跃会话，移除并标记为离线
                userSessionMap.remove(userId);
                Long roomId = userRoomMap.remove(userId);
                if (roomId != null) {
                    log.info("用户 {} 会话已断开，标记为离线，房间ID: {}", userId, roomId);
                    // 广播房间更新，通知其他玩家该玩家已离线
                    broadcastRoomUpdate(roomId);
                }
            } else {
                log.debug("用户 {} 的旧会话 {} 已断开，当前活跃会话: {}", userId, sessionId, currentSessionId);
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
     * 如果用户已有其他会话，会断开旧会话
     */
    public void recordUserJoinRoom(Long userId, Long roomId, String sessionId) {
        if (sessionId != null) {
            // 检查用户是否已有其他会话
            String oldSessionId = userSessionMap.get(userId);
            if (oldSessionId != null && !oldSessionId.equals(sessionId)) {
                log.info("用户 {} 已有会话 {}，断开旧会话，新会话: {}", userId, oldSessionId, sessionId);
                // 断开旧会话
                disconnectSession(oldSessionId);
                // 清理旧会话的映射
                sessionUserMap.remove(oldSessionId);
            }
            
            // 记录新会话
            userSessionMap.put(userId, sessionId);
            sessionUserMap.put(sessionId, userId);
            log.info("记录用户加入房间 - 用户ID: {}, 房间ID: {}, 会话ID: {}", userId, roomId, sessionId);
        }
        
        userRoomMap.put(userId, roomId);
        broadcastRoomUpdate(roomId);
    }
    
    /**
     * 断开指定会话
     * 通过发送断开消息通知客户端，客户端收到后应主动断开连接
     */
    private void disconnectSession(String sessionId) {
        try {
            // 发送断开消息通知客户端
            // 注意：这里使用 /user/{sessionId} 可能不工作，因为 Spring 的 user 目标需要用户名
            // 我们改为使用一个特殊的主题，客户端应该订阅这个主题
            messagingTemplate.convertAndSend("/queue/disconnect/" + sessionId, 
                ApiResponse.error("您的账号在其他地方登录，当前连接已断开"));
            log.info("已发送断开消息到旧会话: sessionId={}", sessionId);
        } catch (Exception e) {
            log.warn("发送断开消息失败: sessionId={}", sessionId, e);
        }
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
     * 根据sessionId获取userId
     */
    public Long getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 获取用户是否在线（根据是否有活跃的WebSocket连接）
     */
    public boolean isUserOnline(Long userId) {
        // 检查用户是否有活跃会话
        String sessionId = userSessionMap.get(userId);
        if (sessionId != null) {
            // 检查会话是否仍然有效
            if (sessionUserMap.containsKey(sessionId)) {
                return true;
            } else {
                // 会话已失效，清理记录
                log.warn("用户 {} 的会话 {} 已失效，清理记录", userId, sessionId);
                userSessionMap.remove(userId);
                userRoomMap.remove(userId);
                return false;
            }
        }
        // 如果没有会话记录，但userRoomMap中有记录，说明可能有问题，清理
        if (userRoomMap.containsKey(userId)) {
            log.warn("用户 {} 的在线状态不一致，清理记录", userId);
            userRoomMap.remove(userId);
        }
        return false;
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

