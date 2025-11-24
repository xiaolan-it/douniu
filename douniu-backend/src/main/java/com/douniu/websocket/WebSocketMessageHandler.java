package com.douniu.websocket;

import com.alibaba.fastjson2.JSON;
import com.douniu.dto.ApiResponse;
import com.douniu.entity.GameDetail;
import com.douniu.entity.GameRecord;
import com.douniu.entity.Room;
import com.douniu.entity.RoomPlayer;
import com.douniu.enums.CardType;
import com.douniu.service.GameService;
import com.douniu.service.RoomService;
import com.douniu.service.UserService;
import com.douniu.utils.CardTypeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final GameService gameService;
    private final UserService userService;
    private final com.douniu.config.WebSocketEventListener webSocketEventListener;

    /**
     * 从消息中获取用户ID的辅助方法
     */
    private Long getUserIdFromMessage(Map<String, Object> payload) {
        // 从payload中获取userId（前端传递）
        if (payload != null && payload.containsKey("userId")) {
            Object userIdObj = payload.get("userId");
            if (userIdObj != null) {
                try {
                    return Long.valueOf(userIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("userId格式错误: {}", userIdObj);
                }
            }
        }

        throw new RuntimeException("无法获取用户ID，请确保消息中包含userId");
    }

    /**
     * 加入房间
     */
    @MessageMapping("/room/join")
    public void joinRoom(@Payload Map<String, Object> payload, org.springframework.messaging.Message<?> message) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            String roomCode = (String) payload.get("roomCode");

            Room room = roomService.getRoomByCode(roomCode);
            if (room == null) {
                sendError(userId, "房间不存在");
                return;
            }

            // 尝试加入房间（如果已在房间中会抛出异常，需要捕获）
            try {
                roomService.joinRoom(room.getId(), userId, null);
            } catch (Exception e) {
                // 如果已在房间中，忽略错误
                if (!e.getMessage().contains("已在房间中")) {
                    sendError(userId, e.getMessage());
                    return;
                }
            }

            // 记录用户加入房间（用于在线状态跟踪）
            // 尝试从消息头获取sessionId
            String sessionId = null;
            try {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    sessionId = accessor.getSessionId();
                }
            } catch (Exception e) {
                log.warn("无法获取sessionId", e);
            }
            
            if (sessionId != null) {
                webSocketEventListener.recordUserJoinRoom(userId, room.getId(), sessionId);
            } else {
                webSocketEventListener.recordUserJoinRoom(userId, room.getId());
            }

            // 广播房间信息更新
            broadcastRoomUpdate(room.getId());

            sendSuccess(userId, "加入房间成功", room);
        } catch (Exception e) {
            log.error("加入房间失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 离开房间
     */
    @MessageMapping("/room/leave")
    public void leaveRoom(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            String roomCode = (String) payload.get("roomCode");

            Room room = roomService.getRoomByCode(roomCode);
            if (room != null) {
                roomService.leaveRoom(room.getId(), userId);
                // 记录用户离开房间
                webSocketEventListener.recordUserLeaveRoom(userId);
                broadcastRoomUpdate(room.getId());
            }

            sendSuccess(userId, "离开房间成功", null);
        } catch (Exception e) {
            log.error("离开房间失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 设置管理员
     */
    @MessageMapping("/room/setAdmin")
    public void setAdmin(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            Long newAdminId = Long.valueOf(payload.get("newAdminId").toString());

            roomService.setAdmin(roomId, userId, newAdminId);
            broadcastRoomUpdate(roomId);
            sendSuccess(userId, "设置管理员成功", null);
        } catch (Exception e) {
            log.error("设置管理员失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 设置庄家
     */
    @MessageMapping("/game/setDealer")
    public void setDealer(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long roomId = Long.valueOf(payload.get("roomId").toString());
            Long dealerId = Long.valueOf(payload.get("dealerId").toString());

            gameService.setDealer(roomId, dealerId);
            
            // 广播房间更新，通知所有玩家庄家已变更
            broadcastRoomUpdate(roomId);
            
            // 额外发送一个专门的庄家变更通知，包含更详细的信息
            List<RoomPlayer> players = roomService.getRoomPlayers(roomId);
            RoomPlayer newDealer = players.stream()
                    .filter(p -> p.getUserId().equals(dealerId))
                    .findFirst()
                    .orElse(null);
            
            Map<String, Object> dealerChangeData = new HashMap<>();
            dealerChangeData.put("dealerId", dealerId);
            dealerChangeData.put("dealerNickname", newDealer != null ? newDealer.getNickname() : "未知");
            dealerChangeData.put("message", "庄家已变更为：" + (newDealer != null ? newDealer.getNickname() : "未知"));
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/dealer/changed",
                    ApiResponse.success(dealerChangeData));
            
            sendSuccess(userId, "设置庄家成功", null);
        } catch (Exception e) {
            log.error("设置庄家失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 开始游戏
     */
    @MessageMapping("/game/start")
    public void startGame(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long roomId = Long.valueOf(payload.get("roomId").toString());

            GameRecord record = gameService.startNewRound(roomId, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("gameRecord", record);
            data.put("roundNumber", record.getRoundNumber());

            // 广播游戏开始
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/start",
                    ApiResponse.success(data));

            broadcastRoomUpdate(roomId);
        } catch (Exception e) {
            log.error("开始游戏失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 投注
     */
    @MessageMapping("/game/bet")
    public void placeBet(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long gameRecordId = Long.valueOf(payload.get("gameRecordId").toString());
            Integer betAmount = Integer.valueOf(payload.get("betAmount").toString());

            gameService.placeBet(gameRecordId, userId, betAmount);

            GameRecord record = gameService.getGameRecord(gameRecordId);
            Long roomId = record.getRoomId();

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("betAmount", betAmount);

            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/bet",
                    ApiResponse.success(data));

            // 检查是否所有玩家都已投注（排除庄家，庄家不需要投注）
            List<RoomPlayer> players = roomService.getRoomPlayers(roomId);
            record = gameService.getGameRecord(gameRecordId);
            Long dealerId = record.getDealerId();

            // 过滤出非庄家玩家
            List<RoomPlayer> nonDealerPlayers = players.stream()
                    .filter(p -> !p.getUserId().equals(dealerId))
                    .collect(Collectors.toList());

            Map<Long, Integer> bets = gameService.getCurrentGameBets(gameRecordId);

            // 如果所有非庄家玩家都投注了，立即自动发牌
            if (bets != null && bets.size() >= nonDealerPlayers.size()) {
                log.info("所有非庄家玩家已投注，立即发牌。投注人数: {}, 非庄家玩家数: {}", bets.size(), nonDealerPlayers.size());
                // 立即发牌（不延迟，确保快速响应）
                new Thread(() -> {
                    try {
                        // 只延迟50ms，确保前端收到投注消息
                        Thread.sleep(50);
                        Map<Long, List<CardTypeCalculator.Card>> cardsMap = gameService.dealCards(gameRecordId);

                        // 广播所有玩家的牌（每个玩家看到自己的4张正面+1张背面，其他玩家5张背面）
                        Map<String, Object> broadcastData = new HashMap<>();
                        Map<Long, Object> allPlayersCards = new HashMap<>();
                        
                        for (RoomPlayer player : players) {
                            List<CardTypeCalculator.Card> playerCards = cardsMap.get(player.getUserId());
                            if (playerCards != null) {
                                Map<String, Object> cardData = new HashMap<>();
                                // 自己的牌：4张正面 + 1张背面
                                cardData.put("cards", playerCards.subList(0, 4)); // 前4张正面
                                cardData.put("hiddenCard", playerCards.get(4)); // 第5张背面
                                cardData.put("isSelf", false); // 标记是否为当前查看者
                                allPlayersCards.put(player.getUserId(), cardData);
                            }
                        }
                        
                        broadcastData.put("cards", allPlayersCards);
                        broadcastData.put("gameRecordId", gameRecordId);
                        
                        // 给每个玩家单独发送，标记自己的牌
                        // 注意：由于没有配置Principal，使用广播方式，但每个玩家根据消息中的isSelf字段区分自己的牌
                        for (RoomPlayer player : players) {
                            Map<String, Object> playerDealData = new HashMap<>();
                            Map<Long, Object> playerCardsMap = new HashMap<>();
                            
                            // 遍历所有玩家
                            for (RoomPlayer p : players) {
                                List<CardTypeCalculator.Card> pCards = cardsMap.get(p.getUserId());
                                if (pCards != null) {
                                    Map<String, Object> cardData = new HashMap<>();
                                    if (p.getUserId().equals(player.getUserId())) {
                                        // 自己的牌：4张正面 + 1张背面
                                        cardData.put("cards", pCards.subList(0, 4));
                                        cardData.put("hiddenCard", pCards.get(4));
                                        cardData.put("isSelf", true);
                                    } else {
                                        // 其他玩家的牌：5张背面
                                        cardData.put("cards", null);
                                        cardData.put("hiddenCard", null);
                                        cardData.put("isSelf", false);
                                        cardData.put("backCount", 5); // 5张背面
                                    }
                                    playerCardsMap.put(p.getUserId(), cardData);
                                }
                            }
                            
                            playerDealData.put("cards", playerCardsMap);
                            playerDealData.put("gameRecordId", gameRecordId);
                            playerDealData.put("targetUserId", player.getUserId()); // 添加目标用户ID，方便前端识别
                            
                            // 直接使用广播方式发送发牌消息（因为点对点消息需要Principal配置，比较复杂）
                            // 每个玩家根据targetUserId过滤自己的消息
                            messagingTemplate.convertAndSend(
                                "/topic/room/" + roomId + "/game/deal/user/" + player.getUserId(),
                                ApiResponse.success(playerDealData)
                            );
                            log.info("发送发牌消息给用户（广播方式）: {}, 主题: /topic/room/{}/game/deal/user/{}", 
                                player.getUserId(), roomId, player.getUserId());
                        }
                        
                        // 广播发牌完成消息（通知所有玩家发牌阶段开始）
                        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/deal", 
                                ApiResponse.success(broadcastData));
                    } catch (Exception e) {
                        log.error("自动发牌失败", e);
                    }
                }).start();
            }
        } catch (Exception e) {
            log.error("投注失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 发牌
     */
    @MessageMapping("/game/deal")
    public void dealCards(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long gameRecordId = Long.valueOf(payload.get("gameRecordId").toString());

            // 检查权限（管理员或所有玩家投注后自动触发）
            Map<Long, List<CardTypeCalculator.Card>> cardsMap = gameService.dealCards(gameRecordId);
            GameRecord record = gameService.getGameRecord(gameRecordId);
            Long roomId = record.getRoomId();

            // 转换为前端需要的格式（只显示4张，隐藏1张）
            Map<String, Object> data = new HashMap<>();
            Map<Long, Object> playerCards = new HashMap<>();
            for (Map.Entry<Long, List<CardTypeCalculator.Card>> entry : cardsMap.entrySet()) {
                List<CardTypeCalculator.Card> cards = entry.getValue();
                Map<String, Object> cardData = new HashMap<>();
                cardData.put("cards", cards.subList(0, 4)); // 前4张
                cardData.put("hiddenCard", cards.get(4)); // 第5张隐藏
                playerCards.put(entry.getKey(), cardData);
            }
            data.put("cards", playerCards);

            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/deal",
                    ApiResponse.success(data));
        } catch (Exception e) {
            log.error("发牌失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 开牌
     */
    @MessageMapping("/game/reveal")
    public void revealCard(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long gameRecordId = Long.valueOf(payload.get("gameRecordId").toString());

            // 记录开牌
            gameService.revealCard(gameRecordId, userId);

            GameRecord record = gameService.getGameRecord(gameRecordId);
            Long roomId = record.getRoomId();
            
            // 获取房间信息以获取启用的牌型
            Room room = roomService.getRoom(roomId);
            List<String> enabledCardTypes = JSON.parseArray(room.getEnabledCardTypes(), String.class);
            Set<String> enabledTypesSet = new HashSet<>(enabledCardTypes);
            
            // 获取该玩家的所有牌
            Map<Long, List<CardTypeCalculator.Card>> cardsMap = gameService.getCurrentGameCards(gameRecordId);
            List<CardTypeCalculator.Card> playerCards = cardsMap.get(userId);
            
            // 计算牌型
            CardType cardType = CardTypeCalculator.calculateCardType(playerCards, enabledTypesSet);
            
            // 计算牌型分组（用于前端显示）
            Map<String, List<CardTypeCalculator.Card>> cardGroups = CardTypeCalculator.getCardGroups(playerCards);

            // 构建开牌数据：包含5张牌、牌型信息和分组信息
            Map<String, Object> revealData = new HashMap<>();
            revealData.put("userId", userId);
            revealData.put("cards", playerCards); // 完整的5张牌
            revealData.put("cardType", cardType.getName()); // 牌型名称
            revealData.put("multiplier", cardType.getMultiplier()); // 赔率
            revealData.put("cardGroups", cardGroups); // 牌型分组（group1: 3张, group2: 2张）

            // 广播给所有玩家（包括自己）
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/reveal",
                    ApiResponse.success(revealData));

            // 检查是否所有玩家都开牌了，如果是则自动结算
            if (gameService.areAllPlayersRevealed(gameRecordId)) {
                // 延迟500ms结算，确保前端收到开牌消息
                final Long finalRoomId = roomId; // 用于lambda表达式
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        Map<Long, GameDetail> details = gameService.settleRound(gameRecordId);
                        GameRecord settledRecord = gameService.getGameRecord(gameRecordId);
                        Room settledRoom = roomService.getRoom(finalRoomId);

                        Map<String, Object> settleData = new HashMap<>();
                        settleData.put("details", details);
                        settleData.put("gameRecord", settledRecord);
                        settleData.put("roomFinished", settledRoom != null && settledRoom.getCurrentRound() >= settledRoom.getMaxRounds());

                        messagingTemplate.convertAndSend("/topic/room/" + finalRoomId + "/game/settle",
                                ApiResponse.success(settleData));

                        broadcastRoomUpdate(finalRoomId);
                    } catch (Exception e) {
                        log.error("自动结算失败", e);
                    }
                }).start();
            }
        } catch (Exception e) {
            log.error("开牌失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 结算
     */
    @MessageMapping("/game/settle")
    public void settleRound(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long gameRecordId = Long.valueOf(payload.get("gameRecordId").toString());

            Map<Long, GameDetail> details = gameService.settleRound(gameRecordId);
            GameRecord record = gameService.getGameRecord(gameRecordId);
            Long roomId = record.getRoomId();

            Map<String, Object> data = new HashMap<>();
            data.put("details", details);
            data.put("gameRecord", record);

            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/settle",
                    ApiResponse.success(data));

            broadcastRoomUpdate(roomId);
        } catch (Exception e) {
            log.error("结算失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    /**
     * 提前结算
     */
    @MessageMapping("/game/finish")
    public void finishGame(@Payload Map<String, Object> payload) {
        Long userId = null;
        try {
            userId = getUserIdFromMessage(payload);
            Long roomId = Long.valueOf(payload.get("roomId").toString());

            gameService.finishGame(roomId, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("roomId", roomId);

            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game/finish",
                    ApiResponse.success(data));

            broadcastRoomUpdate(roomId);
        } catch (Exception e) {
            log.error("提前结算失败", e);
            if (userId != null) {
                sendError(userId, e.getMessage());
            }
        }
    }

    private void broadcastRoomUpdate(Long roomId) {
        Room room = roomService.getRoom(roomId);
        // 获取玩家列表，并填充在线状态
        List<RoomPlayer> players = roomService.getRoomPlayers(roomId, webSocketEventListener::isUserOnline);

        Map<String, Object> data = new HashMap<>();
        data.put("room", room);
        data.put("players", players);

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/update",
                ApiResponse.success(data));
    }

    private void sendSuccess(Long userId, String message, Object data) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/message",
                ApiResponse.success(data));
    }

    private void sendError(Long userId, String message) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/message",
                ApiResponse.error(message));
    }
}

